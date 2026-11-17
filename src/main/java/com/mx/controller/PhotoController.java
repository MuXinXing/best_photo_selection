package com.mx.controller;

import com.mx.entity.PhotoItem;
import com.mx.exception.PhotoSelectionException;
import com.mx.service.ClassificationService;
import com.mx.service.PhotoService;
import com.mx.service.ThumbnailService;
import com.mx.service.WarehouseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.ContentDisposition;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@Log4j2
public class PhotoController {

    private final PhotoService photoService;
    private final ThumbnailService thumbnailService;
    private final ClassificationService classificationService;
    private final WarehouseService warehouseService;

    public PhotoController(PhotoService photoService, ThumbnailService thumbnailService,
                           ClassificationService classificationService, WarehouseService warehouseService) {
        this.photoService = photoService;
        this.thumbnailService = thumbnailService;
        this.classificationService = classificationService;
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "status", required = false, defaultValue = "all") String status,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(name = "order", required = false, defaultValue = "asc") String order,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "60") int size) {
        return ResponseEntity.ok(photoService.listPhotos(status, sort, order, page, size));
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhotoItem> get(@PathVariable("id") String id) {
        return ResponseEntity.ok(photoService.getPhoto(id));
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable("id") String id) {
        File photoFile = photoService.getPhotoFile(id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(photoFile.getName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(photoFile));
    }

    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> thumbnail(
            @PathVariable("id") String id,
            @RequestParam(name = "w", required = false, defaultValue = "400") int width) {
        try {
            PhotoItem item = photoService.requirePhoto(id);
            File thumbFile = thumbnailService.getThumbnail(id, width, item.getRelativePath());
            if (thumbFile == null || !thumbFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(new FileSystemResource(thumbFile));
        } catch (IOException e) {
            log.warn("生成缩略图失败 {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    @PostMapping("/{id}/mark")
    public ResponseEntity<Void> mark(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        if (body == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        photoService.markPhoto(id, body.get("status"));
        return ResponseEntity.ok().build();
    }

    @PostMapping("/batch-mark")
    public ResponseEntity<Map<String, Object>> batchMark(@RequestBody List<Map<String, String>> marks) {
        return ResponseEntity.ok(photoService.batchMark(marks));
    }

    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        return ResponseEntity.ok(photoService.getStatistics());
    }

    @PostMapping("/rescan")
    public ResponseEntity<Map<String, Object>> rescan() {
        photoService.rescan();
        return ResponseEntity.ok(photoService.getStatistics());
    }

    @PostMapping("/classify")
    public ResponseEntity<Map<String, Object>> classify() {
        WarehouseService.Warehouse active = warehouseService.getActiveWarehouse();
        if (active == null) {
            throw new PhotoSelectionException("没有活动片仓", 400);
        }
        if (!classificationService.start(active.getId(), active.getPath())) {
            throw new PhotoSelectionException("分类进行中", 409);
        }
        return ResponseEntity.ok(Map.of("started", true));
    }

    @GetMapping("/classify/progress")
    public ResponseEntity<Map<String, Object>> classifyProgress() {
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("classifying", classificationService.isClassifying());
        result.put("progress", classificationService.getProgress());
        result.put("phase", classificationService.getCurrentPhase());
        result.put("skipped", classificationService.getSkipped());
        return ResponseEntity.ok(result);
    }
}
