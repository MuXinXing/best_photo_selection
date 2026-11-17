package com.mx.service;

import com.mx.util.WarehouseLayout;
import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 缩略图磁盘缓存。宽度限制在 200–1600，同一张图同一宽度同时只生成一次。
 * 缓存放在片仓根的 .thumbnails/{width}/{photoId}.jpg。
 */
@Service
@Log4j2
public class ThumbnailService {

    private static final String THUMBNAIL_DIR = ".thumbnails";

    private final WarehouseService warehouseService;
    private final ConcurrentHashMap<String, Object> generationLocks = new ConcurrentHashMap<>();

    public ThumbnailService(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    public File getThumbnail(String photoId, int width, String relativePath) throws IOException {
        if (photoId == null || !photoId.matches("[A-Za-z0-9_-]+")) {
            throw new IOException("非法照片编号");
        }
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            throw new IOException("没有活动片仓");
        }
        int clamped = WarehouseLayout.clampThumbnailWidth(width);
        Path root = WarehouseLayout.rootPath(active.getPath());
        Path cacheDir = WarehouseLayout.ensureDirInside(root, root.resolve(THUMBNAIL_DIR).resolve(String.valueOf(clamped)));
        Path cache = cacheDir.resolve(photoId + ".jpg").normalize();
        if (!WarehouseLayout.isInside(root, cache)) {
            throw new IOException("缩略图路径超出片仓");
        }
        Object lock = generationLocks.computeIfAbsent(photoId + ":" + clamped, key -> new Object());
        synchronized (lock) {
            if (Files.exists(cache)) {
                return cache.toFile();
            }
            Path scanRoot = WarehouseLayout.scanRoot(active.getPath());
            Path source = scanRoot.resolve(relativePath).normalize();
            if (!WarehouseLayout.isInside(scanRoot, source) || !Files.isRegularFile(source)) {
                throw new IOException("源文件不在片仓内");
            }
            Thumbnails.of(source.toFile())
                    .width(clamped)
                    .outputQuality(0.85)
                    .outputFormat("jpg")
                    .toFile(cache.toFile());
            log.debug("Generated thumbnail: {} ({}px)", cache.getFileName(), clamped);
            return cache.toFile();
        }
    }
}
