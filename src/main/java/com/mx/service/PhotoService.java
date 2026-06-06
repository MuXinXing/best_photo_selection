package com.mx.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import com.mx.entity.PhotoExif;
import com.mx.entity.PhotoItem;
import com.mx.exception.PhotoSelectionException;
import com.mx.util.ExifTool;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
public class PhotoService {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ExifTool exifTool;

    @Autowired
    private ThumbnailService thumbnailService;

    @Autowired
    private ClassificationService classificationService;

    private final ObjectMapper objectMapper = new ObjectMapper();

    /** 照片缓存 key=Base64(id), value=照片对象 */
    private final Map<String, PhotoItem> photoCache = new LinkedHashMap<>();
    /** 全量照片列表，用于排序、过滤、统计 */
    private final List<PhotoItem> photoList = new ArrayList<>();
    /** 精选/落选状态数据，从 .selection_status.json 加载 */
    private StatusData statusData = new StatusData();

    /** 重新扫描当前仓库，重建缓存和状态 */
    public void rescan() {
        photoCache.clear();
        photoList.clear();

        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            log.warn("No active warehouse configured, skipping scan");
            return;
        }

        // 若仓库路径已指向 JPG/ 子目录，状态文件在上级目录
        String scanPath = active.getPath();
        String statusPath = scanPath.endsWith("/JPG") || scanPath.endsWith("\\JPG")
                ? new File(scanPath).getParent().replace("\\", "/")
                : scanPath;

        File warehouseDir = new File(scanPath);
        if (!warehouseDir.exists() || !warehouseDir.isDirectory()) {
            log.warn("Warehouse directory does not exist: {}", scanPath);
            return;
        }

        loadStatusData(statusPath);

        int scanned = 0;
        try (Stream<Path> paths = Files.walk(Paths.get(scanPath))) {
            // 递归扫描仓库目录，只保留 .jpg/.jpeg 文件
            List<Path> jpgFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg");
                    })
                    .filter(p -> !p.startsWith(Paths.get(scanPath, ".thumbnails")))
                    .sorted()
                    .collect(Collectors.toList());

            for (Path jpgPath : jpgFiles) {
                PhotoItem item = buildPhotoItem(jpgPath, scanPath);
                photoCache.put(item.getId(), item);
                photoList.add(item);
                scanned++;
            }
        } catch (IOException e) {
            log.error("Error scanning warehouse: {}", e.getMessage());
        }

        log.info("Scan completed: {} photos found in warehouse {}", scanned, active.getName());

        // 异步预生成缩略图
        final List<PhotoItem> forThumbnail = new ArrayList<>(photoList);
        CompletableFuture.runAsync(() -> {
            // 检查第一张图的缩略图是否已缓存，是则跳过全部预生成
            if (!forThumbnail.isEmpty()) {
                try {
                    File cached = thumbnailService.getThumbnailCacheFile(
                            forThumbnail.get(0).getId(), 1600);
                    if (cached != null && cached.exists()) {
                        log.debug("Thumbnails already cached, skipping pre-generation");
                        return;
                    }
                } catch (Exception ignored) {}
            }
            int done = 0;
            for (PhotoItem item : forThumbnail) {
                try {
                    thumbnailService.getThumbnail(item.getId(), 1600, item.getRelativePath());
                    done++;
                } catch (Exception e) {
                    log.debug("Skip thumbnail for {}: {}", item.getFileName(), e.getMessage());
                }
            }
            log.info("Thumbnails pre-generated: {}/{}", done, forThumbnail.size());
        });
    }

    /** 按状态/排序/分页查询照片，返回 {total,filtered,selected,rejected,unmarked,rows} */
    public Map<String, Object> listPhotos(String status, String sort, String order, int page, int size) {
        if (photoList.isEmpty()) {
            rescan();
        }

        Stream<PhotoItem> stream = photoList.stream();

        // 状态过滤：all / selected / rejected / unmarked
        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            stream = stream.filter(p -> status.equalsIgnoreCase(p.getStatus()));
        }

        // 排序：文件名 / 拍摄日期 / 标记状态
        if (sort != null && !sort.isEmpty()) {
            boolean asc = !"desc".equalsIgnoreCase(order);
            Comparator<PhotoItem> comparator;
            switch (sort.toLowerCase()) {
                case "name":
                    comparator = Comparator.comparing(PhotoItem::getFileName, String.CASE_INSENSITIVE_ORDER);
                    break;
                case "date":
                    comparator = Comparator.comparing(PhotoItem::getDateTaken,
                            Comparator.nullsLast(Comparator.naturalOrder()));
                    break;
                case "status":
                    comparator = Comparator.comparing(PhotoItem::getStatus);
                    break;
                default:
                    comparator = Comparator.comparing(PhotoItem::getFileName);
            }
            stream = asc ? stream.sorted(comparator) : stream.sorted(comparator.reversed());
        }

        List<PhotoItem> all = stream.collect(Collectors.toList());
        int total = photoList.size();
        int filteredTotal = all.size();
        int toIndex;
        if (size <= 0) {
            toIndex = filteredTotal;
        } else {
            int fromIndex = (page - 1) * size;
            toIndex = Math.min(fromIndex + size, filteredTotal);
        }

        List<PhotoItem> rows;
        if (toIndex <= 0) {
            rows = Collections.emptyList();
        } else {
            rows = all.subList(0, toIndex);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("filtered", filteredTotal);
        result.put("selected", countByStatus(PhotoItem.STATUS_SELECTED));
        result.put("rejected", countByStatus(PhotoItem.STATUS_REJECTED));
        result.put("unmarked", countByStatus(PhotoItem.STATUS_UNMARKED));
        result.put("page", page);
        result.put("size", size);
        result.put("rows", rows);
        return result;
    }

    /** 获取单张照片详情（含 EXIF 完整信息） */
    public PhotoItem getPhoto(String id) throws PhotoSelectionException {
        PhotoItem item = photoCache.get(id);
        if (item == null) {
            throw new PhotoSelectionException("Photo not found: " + id);
        }
        loadExifData(item);
        return item;
    }

    /** 标记单张照片：selected / rejected / unmarked */
    public void markPhoto(String id, String status) throws PhotoSelectionException {
        PhotoItem item = photoCache.get(id);
        if (item == null) {
            throw new PhotoSelectionException("Photo not found: " + id);
        }
        item.setStatus(status);
        saveStatus(item);
        if (PhotoItem.STATUS_SELECTED.equals(status)) {
            copySelectedPhotoToSelection(item);
        } else if (PhotoItem.STATUS_UNMARKED.equals(status)) {
            removeSelectedPhotoFromSelection(item);
        }
        log.debug("Marked photo {} as {}", id, status);
    }

    /** 批量标记照片 */
    public void batchMark(List<Map<String, String>> marks) {
        for (Map<String, String> mark : marks) {
            String id = mark.get("id");
            String status = mark.get("status");
            PhotoItem item = photoCache.get(id);
            if (item != null) {
                item.setStatus(status);
                saveStatus(item);
                if (PhotoItem.STATUS_SELECTED.equals(status)) {
                    copySelectedPhotoToSelection(item);
                } else if (PhotoItem.STATUS_UNMARKED.equals(status)) {
                    removeSelectedPhotoFromSelection(item);
                }
            }
        }
        log.info("Batch marked {} photos", marks.size());
    }

    /** 将精选照片的 JPG 和对应 RAW 复制到"选片"目录 */
    private void copySelectedPhotoToSelection(PhotoItem item) {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) return;
        ClassificationService.copyToSelection(active.getPath(), item.getRelativePath());
    }

    /** 当照片被重置时，从"选片"目录中删除对应的 JPG 和 RAW */
    private void removeSelectedPhotoFromSelection(PhotoItem item) {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) return;
        ClassificationService.removeFromSelection(active.getPath(), item.getRelativePath());
    }

    /** 获取全库统计（总数、精选、落选、待筛） */
    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", photoList.size());
        stats.put("selected", countByStatus(PhotoItem.STATUS_SELECTED));
        stats.put("rejected", countByStatus(PhotoItem.STATUS_REJECTED));
        stats.put("unmarked", countByStatus(PhotoItem.STATUS_UNMARKED));
        return stats;
    }

    /** 根据照片 ID 获取文件系统中的原始文件 */
    public File getPhotoFile(String id) throws PhotoSelectionException {
        PhotoItem item = photoCache.get(id);
        if (item == null) {
            throw new PhotoSelectionException("Photo not found: " + id);
        }
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            throw new PhotoSelectionException("No active warehouse");
        }
        return new File(active.getPath(), item.getRelativePath());
    }

    /** 将文件系统路径转换为 PhotoItem 对象 */
    private PhotoItem buildPhotoItem(Path jpgPath, String warehousePath) {
        // 计算相对路径（相对于仓库根目录）
        String relativePath = warehousePath.isEmpty()
                ? jpgPath.toString()
                : Paths.get(warehousePath).relativize(jpgPath).toString();
        relativePath = relativePath.replace("\\", "/");
        String fileName = jpgPath.getFileName().toString();
        // 用 Base64 编码相对路径作为唯一 ID
        String id = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(relativePath.getBytes(StandardCharsets.UTF_8));

        // 恢复上次标记状态（未筛/精选/落选）
        String status = statusData.photos.getOrDefault(relativePath, PhotoItem.STATUS_UNMARKED);
        String dateTaken = null;
        long fileSize = 0;

        try {
            fileSize = Files.size(jpgPath);
        } catch (IOException ignored) {}

        // 读取 EXIF 获取拍摄时间，用于日期排序
        try {
            PhotoExif exif = exifTool.readerSpecificExif(jpgPath.toString());
            if (exif != null) {
                dateTaken = exif.getDateTaken();
            }
        } catch (Exception e) {
            log.debug("Failed to read EXIF from {}: {}", fileName, e.getMessage());
        }

        PhotoItem item = new PhotoItem();
        item.setId(id);
        item.setRelativePath(relativePath);
        item.setFileName(fileName);
        item.setThumbnailUrl("/api/photos/" + id + "/thumbnail?w=1600");
        item.setStatus(status);
        item.setFileSize(fileSize);
        item.setDateTaken(dateTaken);
        return item;
    }

    /** 加载 EXIF 完整信息到 PhotoItem（仅在查看详情时调用） */
    private void loadExifData(PhotoItem item) {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) return;

        String fullPath = active.getPath() + "/" + item.getRelativePath();
        try {
            PhotoExif exif = exifTool.readerSpecificExif(fullPath);
            if (exif != null) {
                item.setCameraModel(exif.getMakerNote());
                item.setFNumber(exif.getFNumber());
                item.setShutterSpeed(exif.getShutterSpeed());
                item.setIsoSpeed(exif.getIsoSpeed());
                item.setFlash(exif.getFlash());
                item.setFocalLength(exif.getFocalLength());
                item.setLenInfo(exif.getLenInfo());
                if (item.getDateTaken() == null) {
                    item.setDateTaken(exif.getDateTaken());
                }
            }
        } catch (Exception e) {
            log.debug("Failed to load EXIF for {}: {}", item.getFileName(), e.getMessage());
        }
    }

    /** 获取状态文件的根路径（若当前仓库路径指向 JPG/，则取上级目录） */
    private String getStatusRootPath() {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) return "";
        String p = active.getPath();
        if (p.endsWith("/JPG") || p.endsWith("\\JPG")) {
            return new File(p).getParent().replace("\\", "/");
        }
        return p;
    }

    /** 持久化单张照片的标记状态 */
    private void saveStatus(PhotoItem item) {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) return;

        statusData.photos.put(item.getRelativePath(), item.getStatus());
        saveStatusData(getStatusRootPath());
    }

    /** 统计某状态的照片数量 */
    private long countByStatus(String status) {
        return photoList.stream().filter(p -> status.equals(p.getStatus())).count();
    }

    /** 从仓库目录加载 .selection_status.json */
    private void loadStatusData(String warehousePath) {
        File statusFile = getStatusFile(warehousePath);
        if (statusFile.exists()) {
            try {
                statusData = objectMapper.readValue(statusFile, StatusData.class);
                log.debug("Loaded status data with {} entries", statusData.photos.size());
            } catch (Exception e) {
                log.warn("Failed to load status data, starting fresh: {}", e.getMessage());
                statusData = new StatusData();
            }
        } else {
            statusData = new StatusData();
        }
    }

    /** 将标记状态写入 .selection_status.json */
    private void saveStatusData(String warehousePath) {
        try {
            File statusFile = getStatusFile(warehousePath);
            statusFile.getParentFile().mkdirs();
            objectMapper.writeValue(statusFile, statusData);
        } catch (Exception e) {
            log.error("Failed to save status data", e);
        }
    }

    private File getStatusFile(String warehousePath) {
        return Paths.get(warehousePath, ".selection_status.json").toFile();
    }

    /** 状态持久化数据结构，JSON 文件自动序列化/反序列化 */
    public static class StatusData {
        public int version = 1;
        public String lastScan = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        /** key=相对路径, value=状态 (selected/rejected/unmarked) */
        public Map<String, String> photos = new LinkedHashMap<>();
    }
}
