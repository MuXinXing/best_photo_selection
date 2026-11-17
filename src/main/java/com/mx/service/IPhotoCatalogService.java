package com.mx.service;

import com.mx.model.bo.PhotoItem;

import java.io.File;
import java.util.List;
import java.util.Map;

/** 照片索引与标记状态的读写。 */
public interface IPhotoCatalogService {

    void rescan();

    Map<String, Object> listPhotos(String status, String sort, String order, int page, int size);

    PhotoItem getPhoto(String id);

    PhotoItem requirePhoto(String id);

    void markPhoto(String id, String status);

    Map<String, Object> batchMark(List<Map<String, String>> marks);

    Map<String, Object> getStatistics();

    File getPhotoFile(String id);
}
