package com.mx.service;

import lombok.extern.log4j.Log4j2;
import net.coobird.thumbnailator.Thumbnails;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/**
 * 缩略图生成与磁盘缓存服务
 *
 * 缓存目录结构: {warehouse}/.thumbnails/{width}/{photoId}.jpg
 * - 首次请求指定宽度的缩略图时，从原图生成并写入缓存
 * - 后续请求直接返回缓存文件，避免重复生成
 */
@Service
@Log4j2
public class ThumbnailService {

    @Autowired
    private WarehouseService warehouseService;

    private static final String THUMBNAIL_DIR = ".thumbnails";

    public File getThumbnail(String photoId, int width, String relativePath) throws IOException {
        File cacheFile = getCacheFile(photoId, width);
        if (cacheFile.exists()) {
            return cacheFile;
        }

        File sourceFile = getSourceFile(relativePath);
        if (!sourceFile.exists()) {
            throw new IOException("Source file not found: " + sourceFile.getAbsolutePath());
        }

        cacheFile.getParentFile().mkdirs();

        Thumbnails.of(sourceFile)
                .width(width)
                .outputQuality(0.85)
                .outputFormat("jpg")
                .toFile(cacheFile);

        log.debug("Generated thumbnail: {} ({}px)", cacheFile.getName(), width);
        return cacheFile;
    }

    /** 获取缩略图缓存文件路径，不触发生成。若仓库未配置返回 null */
    public File getThumbnailCacheFile(String photoId, int width) {
        return getCacheFile(photoId, width);
    }

    private File getCacheFile(String photoId, int width) {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            return null;
        }
        return Paths.get(active.getPath(), THUMBNAIL_DIR, String.valueOf(width), photoId + ".jpg").toFile();
    }

    private File getSourceFile(String relativePath) {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            return null;
        }
        return new File(active.getPath(), relativePath);
    }
}
