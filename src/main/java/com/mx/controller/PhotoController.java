package com.mx.controller;

import com.mx.common.ApiResponse;
import com.mx.logic.PhotoLogic;
import com.mx.model.dto.PhotoMarkDTO;
import com.mx.model.dto.PhotoMarkItemDTO;
import com.mx.model.dto.PhotoQueryDTO;
import com.mx.model.vo.BatchMarkVO;
import com.mx.model.vo.ClassifyProgressVO;
import com.mx.model.vo.PhotoPageVO;
import com.mx.model.vo.PhotoVO;
import com.mx.model.vo.StatisticsVO;
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
import java.util.List;
import java.util.Map;

/** 照片接口。只负责组参和返回，业务在 PhotoLogic。 */
@RestController
@RequestMapping("/api/photos")
@Log4j2
public class PhotoController {

    private final PhotoLogic photoLogic;

    public PhotoController(PhotoLogic photoLogic) {
        this.photoLogic = photoLogic;
    }

    @GetMapping
    public ApiResponse<PhotoPageVO> list(PhotoQueryDTO query) {
        return ApiResponse.ok(photoLogic.list(query));
    }

    @GetMapping("/{id}")
    public ApiResponse<PhotoVO> get(@PathVariable("id") String id) {
        return ApiResponse.ok(photoLogic.detail(id));
    }

    /** 原图是文件流，不包进 JSON。 */
    @GetMapping("/{id}/file")
    public ResponseEntity<Resource> file(@PathVariable("id") String id) {
        File photoFile = photoLogic.photoFile(id);
        ContentDisposition disposition = ContentDisposition.inline()
                .filename(photoFile.getName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_JPEG)
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(new FileSystemResource(photoFile));
    }

    /** 缩略图是文件流，不包进 JSON。 */
    @GetMapping("/{id}/thumbnail")
    public ResponseEntity<Resource> thumbnail(
            @PathVariable("id") String id,
            @RequestParam(name = "w", required = false, defaultValue = "400") int width) {
        try {
            File thumbFile = photoLogic.thumbnail(id, width);
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
    public ApiResponse<Void> mark(@PathVariable("id") String id, @RequestBody PhotoMarkDTO body) {
        if (body == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        photoLogic.mark(id, body.getStatus());
        return ApiResponse.ok(null);
    }

    @PostMapping("/batch-mark")
    public ApiResponse<BatchMarkVO> batchMark(@RequestBody List<PhotoMarkItemDTO> marks) {
        return ApiResponse.ok(photoLogic.batchMark(marks));
    }

    @GetMapping("/statistics")
    public ApiResponse<StatisticsVO> statistics() {
        return ApiResponse.ok(photoLogic.statistics());
    }

    @PostMapping("/rescan")
    public ApiResponse<StatisticsVO> rescan() {
        return ApiResponse.ok(photoLogic.rescan());
    }

    @PostMapping("/classify")
    public ApiResponse<Map<String, Boolean>> classify() {
        photoLogic.startClassify();
        return ApiResponse.ok(Map.of("started", true));
    }

    @GetMapping("/classify/progress")
    public ApiResponse<ClassifyProgressVO> classifyProgress() {
        return ApiResponse.ok(photoLogic.classifyProgress());
    }
}
