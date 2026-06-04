package com.mx.controller;

import com.mx.entity.PhotoItem;
import com.mx.exception.PhotoSelectionException;
import com.mx.service.PhotoService;
import com.mx.service.ThumbnailService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/photos")
@Log4j2
public class PhotoController {

    @Autowired
    private PhotoService photoService;

    @Autowired
    private ThumbnailService thumbnailService;

    /** 照片列表：支持按状态过滤、按字段排序、分页 */
    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(name = "status", required = false, defaultValue = "all") String status,
            @RequestParam(name = "sort", required = false, defaultValue = "name") String sort,
            @RequestParam(name = "order", required = false, defaultValue = "asc") String order,
            @RequestParam(name = "page", required = false, defaultValue = "1") int page,
            @RequestParam(name = "size", required = false, defaultValue = "60") int size) {
        Map<String, Object> result = photoService.listPhotos(status, sort, order, page, size);
        return ResponseEntity.ok(result);
    }

    /** 单张照片详情（含完整 EXIF 信息） */
    @GetMapping("/{id}")
    public ResponseEntity<PhotoItem> get(@PathVariable("id") String id) {
        try {
            PhotoItem item = photoService.getPhoto(id);
            return ResponseEntity.ok(item);
        } catch (PhotoSelectionException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 返回 JPG 原图文件流（inline 方式在浏览器中直接显示） */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable("id") String id) {
        try {
            File photoFile = photoService.getPhotoFile(id);
            if (!photoFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(photoFile);
            String contentType = "image/jpeg";
            return ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(contentType))
                    .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename=\"" + photoFile.getName() + "\"")
                    .body(resource);
        } catch (PhotoSelectionException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 返回指定宽度的缩略图（首次请求自动生成并缓存到磁盘） */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> thumbnail(
            @PathVariable("id") String id,
            @RequestParam(name = "w", required = false, defaultValue = "400") int width) {
        try {
            PhotoItem item = photoService.getPhoto(id);
            File thumbFile = thumbnailService.getThumbnail(id, width, item.getRelativePath());
            if (thumbFile == null || !thumbFile.exists()) {
                return ResponseEntity.notFound().build();
            }
            Resource resource = new FileSystemResource(thumbFile);
            return ResponseEntity.ok()
                    .contentType(MediaType.IMAGE_JPEG)
                    .body(resource);
        } catch (Exception e) {
            log.warn("Failed to generate thumbnail for {}: {}", id, e.getMessage());
            return ResponseEntity.notFound().build();
        }
    }

    /** 标记单张照片：selected / rejected / unmarked */
    @PostMapping("/{id}/mark")
    public ResponseEntity<Void> mark(@PathVariable("id") String id, @RequestBody Map<String, String> body) {
        try {
            String status = body.get("status");
            if (status == null || (!PhotoItem.STATUS_SELECTED.equals(status)
                    && !PhotoItem.STATUS_REJECTED.equals(status)
                    && !PhotoItem.STATUS_UNMARKED.equals(status))) {
                return ResponseEntity.badRequest().build();
            }
            photoService.markPhoto(id, status);
            return ResponseEntity.ok().build();
        } catch (PhotoSelectionException e) {
            return ResponseEntity.notFound().build();
        }
    }

    /** 批量标记照片 */
    @PostMapping("/batch-mark")
    public ResponseEntity<Void> batchMark(@RequestBody List<Map<String, String>> marks) {
        photoService.batchMark(marks);
        return ResponseEntity.ok().build();
    }

    /** 获取全库统计数量 */
    @GetMapping("/statistics")
    public ResponseEntity<Map<String, Object>> statistics() {
        return ResponseEntity.ok(photoService.getStatistics());
    }

    /** 触发仓库重新扫描 */
    @PostMapping("/rescan")
    public ResponseEntity<Map<String, Object>> rescan() {
        photoService.rescan();
        return ResponseEntity.ok(photoService.getStatistics());
    }
}
