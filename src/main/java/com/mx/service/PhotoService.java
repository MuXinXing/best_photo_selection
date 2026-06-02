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
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Service
@Log4j2
public class PhotoService {

    @Autowired
    private WarehouseService warehouseService;

    @Autowired
    private ExifTool exifTool;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final Map<String, PhotoItem> photoCache = new LinkedHashMap<>();
    private final List<PhotoItem> photoList = new ArrayList<>();
    private StatusData statusData = new StatusData();

    public void rescan() {
        photoCache.clear();
        photoList.clear();

        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            log.warn("No active warehouse configured, skipping scan");
            return;
        }

        File warehouseDir = new File(active.getPath());
        if (!warehouseDir.exists() || !warehouseDir.isDirectory()) {
            log.warn("Warehouse directory does not exist: {}", active.getPath());
            return;
        }

        loadStatusData(active.getPath());

        int scanned = 0;
        try (Stream<Path> paths = Files.walk(Paths.get(active.getPath()))) {
            List<Path> jpgFiles = paths
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg");
                    })
                    .sorted()
                    .collect(Collectors.toList());

            for (Path jpgPath : jpgFiles) {
                PhotoItem item = buildPhotoItem(jpgPath, active.getPath());
                photoCache.put(item.getId(), item);
                photoList.add(item);
                scanned++;
            }
        } catch (IOException e) {
            log.error("Error scanning warehouse: {}", e.getMessage());
        }

        log.info("Scan completed: {} photos found in warehouse {}", scanned, active.getName());
    }

    public Map<String, Object> listPhotos(String status, String sort, String order, int page, int size) {
        if (photoList.isEmpty()) {
            rescan();
        }

        Stream<PhotoItem> stream = photoList.stream();

        if (status != null && !status.isEmpty() && !"all".equalsIgnoreCase(status)) {
            stream = stream.filter(p -> status.equalsIgnoreCase(p.getStatus()));
        }

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
        int total = all.size();
        int fromIndex = (page - 1) * size;
        int toIndex = Math.min(fromIndex + size, total);

        List<PhotoItem> rows;
        if (fromIndex >= total) {
            rows = Collections.emptyList();
        } else {
            rows = all.subList(fromIndex, toIndex);
        }

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("total", total);
        result.put("selected", countByStatus(PhotoItem.STATUS_SELECTED));
        result.put("rejected", countByStatus(PhotoItem.STATUS_REJECTED));
        result.put("unmarked", countByStatus(PhotoItem.STATUS_UNMARKED));
        result.put("page", page);
        result.put("size", size);
        result.put("rows", rows);
        return result;
    }

    public PhotoItem getPhoto(String id) throws PhotoSelectionException {
        PhotoItem item = photoCache.get(id);
        if (item == null) {
            throw new PhotoSelectionException("Photo not found: " + id);
        }
        loadExifData(item);
        return item;
    }

    public void markPhoto(String id, String status) throws PhotoSelectionException {
        PhotoItem item = photoCache.get(id);
        if (item == null) {
            throw new PhotoSelectionException("Photo not found: " + id);
        }
        item.setStatus(status);
        saveStatus(item);
        log.debug("Marked photo {} as {}", id, status);
    }

    public void batchMark(List<Map<String, String>> marks) {
        for (Map<String, String> mark : marks) {
            String id = mark.get("id");
            String status = mark.get("status");
            PhotoItem item = photoCache.get(id);
            if (item != null) {
                item.setStatus(status);
                saveStatus(item);
            }
        }
        log.info("Batch marked {} photos", marks.size());
    }

    public Map<String, Object> getStatistics() {
        Map<String, Object> stats = new LinkedHashMap<>();
        stats.put("total", photoList.size());
        stats.put("selected", countByStatus(PhotoItem.STATUS_SELECTED));
        stats.put("rejected", countByStatus(PhotoItem.STATUS_REJECTED));
        stats.put("unmarked", countByStatus(PhotoItem.STATUS_UNMARKED));
        return stats;
    }

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

    private PhotoItem buildPhotoItem(Path jpgPath, String warehousePath) {
        String relativePath = warehousePath.isEmpty()
                ? jpgPath.toString()
                : Paths.get(warehousePath).relativize(jpgPath).toString();
        relativePath = relativePath.replace("\\", "/");
        String fileName = jpgPath.getFileName().toString();
        String id = Base64.getUrlEncoder().withoutPadding()
                .encodeToString(relativePath.getBytes(StandardCharsets.UTF_8));

        String status = statusData.photos.getOrDefault(relativePath, PhotoItem.STATUS_UNMARKED);
        String dateTaken = null;
        long fileSize = 0;

        try {
            fileSize = Files.size(jpgPath);
        } catch (IOException ignored) {}

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
        item.setThumbnailUrl("/api/photos/" + id + "/file");
        item.setStatus(status);
        item.setFileSize(fileSize);
        item.setDateTaken(dateTaken);
        return item;
    }

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

    private void saveStatus(PhotoItem item) {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) return;

        statusData.photos.put(item.getRelativePath(), item.getStatus());
        saveStatusData(active.getPath());
    }

    private long countByStatus(String status) {
        return photoList.stream().filter(p -> status.equals(p.getStatus())).count();
    }

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

    public static class StatusData {
        public int version = 1;
        public String lastScan = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss"));
        public Map<String, String> photos = new LinkedHashMap<>();
    }
}
