package com.mx.logic;

import com.mx.common.PhotoSelectionException;
import com.mx.logic.storage.ClassificationStorage;
import com.mx.logic.storage.ThumbnailStorage;
import com.mx.model.bo.PhotoItem;
import com.mx.model.bo.WarehouseBO;
import com.mx.model.dto.PhotoMarkItemDTO;
import com.mx.model.dto.PhotoQueryDTO;
import com.mx.model.vo.BatchMarkVO;
import com.mx.model.vo.ClassifyProgressVO;
import com.mx.model.vo.PhotoPageVO;
import com.mx.model.vo.PhotoVO;
import com.mx.model.vo.StatisticsVO;
import com.mx.service.IPhotoCatalogService;
import com.mx.service.IWarehouseService;
import org.springframework.stereotype.Component;

import java.io.File;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 照片浏览、标记和文件整理的业务入口。 */
@Component
public class PhotoLogic {

    private static final int MAX_PAGE_SIZE = 50;

    private final IPhotoCatalogService photoCatalogService;
    private final IWarehouseService warehouseService;
    private final ClassificationStorage classificationStorage;
    private final ThumbnailStorage thumbnailStorage;

    public PhotoLogic(IPhotoCatalogService photoCatalogService, IWarehouseService warehouseService,
                      ClassificationStorage classificationStorage, ThumbnailStorage thumbnailStorage) {
        this.photoCatalogService = photoCatalogService;
        this.warehouseService = warehouseService;
        this.classificationStorage = classificationStorage;
        this.thumbnailStorage = thumbnailStorage;
    }

    public PhotoPageVO list(PhotoQueryDTO query) {
        PhotoQueryDTO safe = query == null ? new PhotoQueryDTO() : query;
        int page = safe.getPage() == null || safe.getPage() < 1 ? 1 : safe.getPage();
        int pageSize = clampPageSize(safe.getPageSize());
        Map<String, Object> raw = photoCatalogService.listPhotos(
                safe.getStatus(), safe.getSort(), safe.getOrder(), page, pageSize);
        PhotoPageVO vo = new PhotoPageVO();
        vo.setList(toPhotoList(raw.get("rows")));
        vo.setTotal(number(raw.get("filtered")));
        vo.setLibraryTotal(number(raw.get("total")));
        vo.setSelected(number(raw.get("selected")));
        vo.setRejected(number(raw.get("rejected")));
        vo.setUnmarked(number(raw.get("unmarked")));
        vo.setPage(page);
        vo.setPageSize(pageSize);
        return vo;
    }

    public PhotoVO detail(String id) {
        return toVo(photoCatalogService.getPhoto(id));
    }

    public void mark(String id, String status) {
        ensureNotClassifying();
        photoCatalogService.markPhoto(id, status);
    }

    public BatchMarkVO batchMark(List<PhotoMarkItemDTO> marks) {
        if (marks == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        ensureNotClassifying();
        List<Map<String, String>> raw = new ArrayList<>();
        for (PhotoMarkItemDTO item : marks) {
            Map<String, String> one = new LinkedHashMap<>();
            one.put("id", item == null || item.getId() == null ? "" : item.getId());
            one.put("status", item == null ? null : item.getStatus());
            raw.add(one);
        }
        Map<String, Object> result = photoCatalogService.batchMark(raw);
        BatchMarkVO vo = new BatchMarkVO();
        vo.setUpdated(((Number) result.get("updated")).intValue());
        Object failed = result.get("failed");
        if (failed instanceof List<?> list) {
            vo.setFailed(list.stream().map(String::valueOf).toList());
        }
        return vo;
    }

    public StatisticsVO statistics() {
        return toStatistics(photoCatalogService.getStatistics());
    }

    public StatisticsVO rescan() {
        photoCatalogService.rescan();
        return statistics();
    }

    public File photoFile(String id) {
        return photoCatalogService.getPhotoFile(id);
    }

    public File thumbnail(String id, int width) throws java.io.IOException {
        PhotoItem item = photoCatalogService.requirePhoto(id);
        return thumbnailStorage.getThumbnail(id, width, item.getRelativePath());
    }

    public boolean startClassify() {
        WarehouseBO active = warehouseService.getActiveWarehouse();
        if (active == null) {
            throw new PhotoSelectionException("没有活动片仓", 400);
        }
        if (!classificationStorage.start(active.getId(), active.getPath())) {
            throw new PhotoSelectionException("分类进行中", 409);
        }
        return true;
    }

    public ClassifyProgressVO classifyProgress() {
        ClassifyProgressVO vo = new ClassifyProgressVO();
        vo.setClassifying(classificationStorage.isClassifying());
        vo.setProgress(classificationStorage.getProgress());
        vo.setPhase(classificationStorage.getCurrentPhase());
        vo.setSkipped(classificationStorage.getSkipped());
        return vo;
    }

    private void ensureNotClassifying() {
        if (classificationStorage.isClassifying()) {
            throw new PhotoSelectionException("仓库正在整理文件，请稍后再标记", 409);
        }
    }

    private static int clampPageSize(Integer pageSize) {
        int size = pageSize == null ? 10 : pageSize;
        if (size < 1) {
            return 10;
        }
        return Math.min(size, MAX_PAGE_SIZE);
    }

    private static long number(Object value) {
        return value instanceof Number number ? number.longValue() : 0L;
    }

    @SuppressWarnings("unchecked")
    private List<PhotoVO> toPhotoList(Object rows) {
        if (!(rows instanceof List<?> list)) {
            return List.of();
        }
        List<PhotoVO> result = new ArrayList<>();
        for (Object item : list) {
            if (item instanceof PhotoItem photo) {
                result.add(toVo(photo));
            }
        }
        return result;
    }

    private PhotoVO toVo(PhotoItem source) {
        PhotoVO vo = new PhotoVO();
        vo.setId(source.getId());
        vo.setFileName(source.getFileName());
        vo.setThumbnailUrl(source.getThumbnailUrl());
        vo.setStatus(source.getStatus());
        vo.setFileSize(source.getFileSize());
        vo.setDateTaken(source.getDateTaken());
        vo.setCameraModel(source.getCameraModel());
        vo.setFNumber(source.getFNumber());
        vo.setShutterSpeed(source.getShutterSpeed());
        vo.setIsoSpeed(source.getIsoSpeed());
        vo.setFlash(source.getFlash());
        vo.setFocalLength(source.getFocalLength());
        vo.setLenInfo(source.getLenInfo());
        return vo;
    }

    private StatisticsVO toStatistics(Map<String, Object> raw) {
        StatisticsVO vo = new StatisticsVO();
        vo.setTotal(number(raw.get("total")));
        vo.setSelected(number(raw.get("selected")));
        vo.setRejected(number(raw.get("rejected")));
        vo.setUnmarked(number(raw.get("unmarked")));
        return vo;
    }
}
