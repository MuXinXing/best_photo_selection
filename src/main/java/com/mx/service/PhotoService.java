package com.mx.service;

import com.mx.entity.PhotoExif;
import com.mx.entity.PhotoItem;
import com.mx.event.ActiveWarehouseChangedEvent;
import com.mx.exception.PhotoSelectionException;
import com.mx.util.ExifTool;
import com.mx.util.WarehouseFiles;
import com.mx.util.WarehouseLayout;
import jakarta.annotation.PreDestroy;
import lombok.extern.log4j.Log4j2;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import tools.jackson.databind.ObjectMapper;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.AtomicMoveNotSupportedException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Base64;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Log4j2
public class PhotoService {

    private static final DateTimeFormatter SCAN_TIME = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    private final WarehouseService warehouseService;
    private final ExifTool exifTool;
    private final ClassificationService classificationService;
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ReentrantLock lock = new ReentrantLock();
    private final ExecutorService exifExecutor = Executors.newFixedThreadPool(2, runnable -> {
        Thread thread = new Thread(runnable, "exif-date");
        thread.setDaemon(true);
        return thread;
    });

    private final Map<String, PhotoItem> photoCache = new LinkedHashMap<>();
    private final List<PhotoItem> photoList = new ArrayList<>();
    private StatusData statusData = new StatusData();
    private boolean scanCompleted;
    private int scanGeneration;

    public PhotoService(WarehouseService warehouseService, ExifTool exifTool, ClassificationService classificationService) {
        this.warehouseService = warehouseService;
        this.exifTool = exifTool;
        this.classificationService = classificationService;
    }

    @PreDestroy
    public void shutdown() {
        exifExecutor.shutdownNow();
    }

    @EventListener
    public void onActiveWarehouseChanged(ActiveWarehouseChangedEvent event) {
        rescan();
    }

    public void rescan() {
        List<DateJob> jobs;
        lock.lock();
        try {
            jobs = rebuildIndex();
        } finally {
            lock.unlock();
        }
        submit(jobs);
    }

    public Map<String, Object> listPhotos(String status, String sort, String order, int page, int size) {
        List<DateJob> jobs = List.of();
        Map<String, Object> result;
        lock.lock();
        try {
            if (!scanCompleted) {
                jobs = rebuildIndex();
            }
            result = buildPage(status, sort, order, page, size);
        } finally {
            lock.unlock();
        }
        submit(jobs);
        return result;
    }

    public PhotoItem getPhoto(String id) {
        PhotoItem item = requirePhoto(id);
        loadExifData(item);
        return item;
    }

    public PhotoItem requirePhoto(String id) {
        List<DateJob> jobs = List.of();
        PhotoItem item;
        lock.lock();
        try {
            if (!scanCompleted) {
                jobs = rebuildIndex();
            }
            item = photoCache.get(id);
        } finally {
            lock.unlock();
        }
        submit(jobs);
        if (item == null) {
            throw new PhotoSelectionException("照片不存在");
        }
        return item;
    }

    public void markPhoto(String id, String status) {
        if (!PhotoItem.isValidStatus(status)) {
            throw new IllegalArgumentException("不支持的标记状态");
        }
        if (classificationService.isClassifying()) {
            throw new PhotoSelectionException("仓库正在整理文件，请稍后再标记", 409);
        }
        List<DateJob> jobs = List.of();
        try {
            lock.lock();
            try {
                if (!scanCompleted) {
                    jobs = rebuildIndex();
                }
                PhotoItem item = photoCache.get(id);
                if (item == null) {
                    throw new PhotoSelectionException("照片不存在");
                }
                WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
                if (active == null) {
                    throw new PhotoSelectionException("没有活动片仓", 400);
                }
                String previous = item.getStatus();
                try {
                    if (PhotoItem.STATUS_SELECTED.equals(status) && !PhotoItem.STATUS_SELECTED.equals(previous)) {
                        WarehouseFiles.copySelected(active.getPath(), item.getRelativePath());
                    } else if (PhotoItem.STATUS_SELECTED.equals(previous) && !PhotoItem.STATUS_SELECTED.equals(status)) {
                        WarehouseFiles.removeSelected(active.getPath(), item.getRelativePath());
                    }
                } catch (IOException e) {
                    throw new PhotoSelectionException("更新选片文件失败", 500, e);
                }
                item.setStatus(status);
                statusData.photos.put(item.getRelativePath(), status);
                try {
                    saveStatusData(active.getPath());
                } catch (RuntimeException e) {
                    item.setStatus(previous);
                    statusData.photos.put(item.getRelativePath(), previous);
                    throw e;
                }
            } finally {
                lock.unlock();
            }
        } finally {
            submit(jobs);
        }
    }

    public Map<String, Object> batchMark(List<Map<String, String>> marks) {
        if (marks == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        List<String> failed = new ArrayList<>();
        int updated = 0;
        for (Map<String, String> mark : marks) {
            String id = mark == null ? "" : mark.getOrDefault("id", "");
            String status = mark == null ? null : mark.get("status");
            try {
                markPhoto(id, status);
                updated++;
            } catch (RuntimeException e) {
                failed.add(id);
                log.debug("批量标记跳过 {}: {}", id, e.getMessage());
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("updated", updated);
        result.put("failed", failed);
        return result;
    }

    public Map<String, Object> getStatistics() {
        List<DateJob> jobs = List.of();
        Map<String, Object> stats = new LinkedHashMap<>();
        lock.lock();
        try {
            if (!scanCompleted) {
                jobs = rebuildIndex();
            }
            stats.put("total", photoList.size());
            stats.put("selected", countByStatus(PhotoItem.STATUS_SELECTED));
            stats.put("rejected", countByStatus(PhotoItem.STATUS_REJECTED));
            stats.put("unmarked", countByStatus(PhotoItem.STATUS_UNMARKED));
        } finally {
            lock.unlock();
        }
        submit(jobs);
        return stats;
    }

    public File getPhotoFile(String id) {
        PhotoItem item = requirePhoto(id);
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            throw new PhotoSelectionException("没有活动片仓", 400);
        }
        Path scanRoot = WarehouseLayout.scanRoot(active.getPath());
        Path candidate = scanRoot.resolve(item.getRelativePath()).normalize();
        if (!WarehouseLayout.isInside(scanRoot, candidate) || !Files.isRegularFile(candidate)) {
            throw new PhotoSelectionException("照片文件不存在");
        }
        return candidate.toFile();
    }

    private void submit(List<DateJob> jobs) {
        for (DateJob job : jobs) {
            exifExecutor.execute(() -> fillDateTaken(job));
        }
    }

    private void fillDateTaken(DateJob job) {
        String dateTaken = exifTool.readDateTaken(job.absolutePath());
        if (dateTaken == null || dateTaken.isBlank()) {
            return;
        }
        lock.lock();
        try {
            if (job.generation() != scanGeneration) {
                return;
            }
            PhotoItem item = photoCache.get(job.id());
            if (item != null && item.getDateTaken() == null) {
                item.setDateTaken(dateTaken);
            }
        } finally {
            lock.unlock();
        }
    }

    /** 调用方必须持有 lock。 */
    private List<DateJob> rebuildIndex() {
        photoCache.clear();
        photoList.clear();
        int generation = ++scanGeneration;
        List<DateJob> jobs = new ArrayList<>();
        try {
            WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
            if (active == null) {
                statusData = new StatusData();
                return jobs;
            }
            Path scanRoot = WarehouseLayout.scanRoot(active.getPath());
            if (!Files.isDirectory(scanRoot)) {
                log.warn("片仓目录不存在: {}", scanRoot);
                statusData = new StatusData();
                return jobs;
            }
            loadStatusData(active.getPath());
            List<Path> jpgFiles = new ArrayList<>();
            Files.walkFileTree(scanRoot, new SimpleFileVisitor<>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (dir.equals(scanRoot)) {
                        return FileVisitResult.CONTINUE;
                    }
                    String name = dir.getFileName().toString();
                    if (".thumbnails".equals(name) || "选片".equals(name) || "RAW".equals(name)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    return FileVisitResult.CONTINUE;
                }

                @Override
                public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) {
                    if (includePhoto(scanRoot, file)) {
                        jpgFiles.add(file);
                    }
                    return FileVisitResult.CONTINUE;
                }
            });
            jpgFiles.sort(Comparator.comparing(path -> path.getFileName().toString()));
            for (Path jpgPath : jpgFiles) {
                PhotoItem item = buildPhotoItem(jpgPath, scanRoot);
                photoCache.put(item.getId(), item);
                photoList.add(item);
                jobs.add(new DateJob(generation, item.getId(), jpgPath.toString()));
            }
            log.info("扫描完成: {} 张照片，片仓 {}", photoList.size(), active.getName());
            return jobs;
        } catch (IOException e) {
            log.error("扫描片仓失败: {}", e.getMessage());
            return jobs;
        } finally {
            scanCompleted = true;
        }
    }

    private boolean includePhoto(Path scanRoot, Path path) {
        try {
            Path fileName = path.getFileName();
            if (fileName == null || !WarehouseFiles.isJpg(fileName.toString())) {
                return false;
            }
            Path relative = scanRoot.relativize(path.normalize());
            if (relative.startsWith("..")) {
                return false;
            }
            for (Path part : relative) {
                String name = part.toString();
                if ("..".equals(name) || ".thumbnails".equals(name) || "选片".equals(name)) {
                    return false;
                }
            }
            if (Files.isSymbolicLink(path)) {
                Path real = path.toRealPath().normalize();
                Path scanReal = scanRoot.toRealPath().normalize();
                return real.startsWith(scanReal) && Files.isRegularFile(real);
            }
            return Files.isRegularFile(path, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            return false;
        }
    }

    private PhotoItem buildPhotoItem(Path jpgPath, Path scanRoot) {
        String relativePath = scanRoot.relativize(jpgPath).toString().replace('\\', '/');
        String fileName = jpgPath.getFileName().toString();
        String id = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(relativePath.getBytes(StandardCharsets.UTF_8));
        String status = statusData.photos.getOrDefault(relativePath, PhotoItem.STATUS_UNMARKED);
        if (!PhotoItem.isValidStatus(status)) {
            status = PhotoItem.STATUS_UNMARKED;
        }
        long fileSize = 0;
        try {
            fileSize = Files.size(jpgPath);
        } catch (IOException ignored) {
            fileSize = 0;
        }
        PhotoItem item = new PhotoItem();
        item.setId(id);
        item.setRelativePath(relativePath);
        item.setFileName(fileName);
        item.setThumbnailUrl("/api/photos/" + id + "/thumbnail?w=1600");
        item.setStatus(status);
        item.setFileSize(fileSize);
        return item;
    }

    private Map<String, Object> buildPage(String status, String sort, String order, int page, int size) {
        List<PhotoItem> filtered = new ArrayList<>(photoList);
        if (status != null && !status.isBlank() && !"all".equalsIgnoreCase(status)) {
            filtered.removeIf(item -> !status.equalsIgnoreCase(item.getStatus()));
        }
        if (sort != null && !sort.isBlank()) {
            Comparator<PhotoItem> comparator = switch (sort.toLowerCase(Locale.ROOT)) {
                case "date" -> Comparator.comparing(PhotoItem::getDateTaken, Comparator.nullsLast(Comparator.naturalOrder()));
                case "status" -> Comparator.comparing(item -> item.getStatus() == null ? "" : item.getStatus());
                default -> Comparator.comparing(PhotoItem::getFileName, String.CASE_INSENSITIVE_ORDER);
            };
            if ("desc".equalsIgnoreCase(order)) {
                comparator = comparator.reversed();
            }
            filtered.sort(comparator);
        }
        int safePage = Math.max(page, 1);
        List<PhotoItem> rows;
        if (size <= 0) {
            rows = filtered;
        } else {
            int fromIndex = (safePage - 1) * size;
            if (fromIndex >= filtered.size()) {
                rows = List.of();
            } else {
                rows = new ArrayList<>(filtered.subList(fromIndex, Math.min(fromIndex + size, filtered.size())));
            }
        }
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", photoList.size());
        result.put("filtered", filtered.size());
        result.put("selected", countByStatus(PhotoItem.STATUS_SELECTED));
        result.put("rejected", countByStatus(PhotoItem.STATUS_REJECTED));
        result.put("unmarked", countByStatus(PhotoItem.STATUS_UNMARKED));
        result.put("page", safePage);
        result.put("size", size);
        result.put("rows", rows);
        return result;
    }

    private void loadExifData(PhotoItem item) {
        try {
            File file = getPhotoFile(item.getId());
            PhotoExif exif = exifTool.readerSpecificExif(file.getAbsolutePath());
            item.setCameraModel(exif.getCameraModel());
            item.setFNumber(exif.getFNumber());
            item.setShutterSpeed(exif.getShutterSpeed());
            item.setIsoSpeed(exif.getIsoSpeed());
            item.setFlash(exif.getFlash());
            item.setFocalLength(exif.getFocalLength());
            item.setLenInfo(exif.getLenInfo());
            if (item.getDateTaken() == null) {
                item.setDateTaken(exif.getDateTaken());
            }
        } catch (RuntimeException e) {
            log.debug("读取照片 EXIF 失败 {}: {}", item.getFileName(), e.getMessage());
        }
    }

    private long countByStatus(String status) {
        return photoList.stream().filter(item -> status.equals(item.getStatus())).count();
    }

    private void loadStatusData(String activePath) {
        File statusFile = statusFile(activePath);
        if (!statusFile.exists()) {
            statusData = new StatusData();
            return;
        }
        try {
            statusData = objectMapper.readValue(statusFile, StatusData.class);
            if (statusData.photos == null) {
                statusData.photos = new LinkedHashMap<>();
            }
        } catch (Exception e) {
            log.warn("读取标记文件失败，将从空白状态开始: {}", e.getMessage());
            statusData = new StatusData();
        }
    }

    private void saveStatusData(String activePath) {
        try {
            File statusFile = statusFile(activePath);
            Path parent = statusFile.toPath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            statusData.lastScan = LocalDateTime.now().format(SCAN_TIME);
            Path tmp = statusFile.toPath().resolveSibling(statusFile.getName() + ".tmp");
            objectMapper.writeValue(tmp.toFile(), statusData);
            moveReplacing(tmp, statusFile.toPath());
        } catch (Exception e) {
            log.error("保存标记文件失败", e);
            throw new PhotoSelectionException("保存标记失败", 500, e);
        }
    }

    private static void moveReplacing(Path tmp, Path target) throws IOException {
        try {
            Files.move(tmp, target, StandardCopyOption.ATOMIC_MOVE, StandardCopyOption.REPLACE_EXISTING);
        } catch (AtomicMoveNotSupportedException e) {
            Files.move(tmp, target, StandardCopyOption.REPLACE_EXISTING);
        }
    }

    private File statusFile(String activePath) {
        return WarehouseLayout.rootPath(activePath).resolve(".selection_status.json").toFile();
    }

    private record DateJob(int generation, String id, String absolutePath) {
    }

    public static class StatusData {
        public int version = 1;
        public String lastScan = "";
        public Map<String, String> photos = new LinkedHashMap<>();
    }
}
