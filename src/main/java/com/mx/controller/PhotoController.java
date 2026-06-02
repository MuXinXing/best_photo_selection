package com.mx.controller;

import com.mx.entity.PhotoItem;
import com.mx.exception.PhotoSelectionException;
import com.mx.service.PhotoService;
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

    @GetMapping
    public ResponseEntity<Map<String, Object>> list(
            @RequestParam(required = false, defaultValue = "all") String status,
            @RequestParam(required = false, defaultValue = "name") String sort,
            @RequestParam(required = false, defaultValue = "asc") String order,
            @RequestParam(required = false, defaultValue = "1") int page,
            @RequestParam(required = false, defaultValue = "60") int size) {
        Map<String, Object> result = photoService.listPhotos(status, sort, order, page, size);
        return ResponseEntity.ok(result);
    }

    @GetMapping("/{id}")
    public ResponseEntity<PhotoItem> get(@PathVariable String id) {
        try {
            PhotoItem item = photoService.getPhoto(id);
            return ResponseEntity.ok(item);
        } catch (PhotoSelectionException e) {
            return ResponseEntity.notFound().build();
        }
    }

    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable String id) {
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

    @PostMapping("/{id}/mark")
    public ResponseEntity<Void> mark(@PathVariable String id, @RequestBody Map<String, String> body) {
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

    @PostMapping("/batch-mark")
    public ResponseEntity<Void> batchMark(@RequestBody List<Map<String, String>> marks) {
        photoService.batchMark(marks);
        return ResponseEntity.ok().build();
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
}
