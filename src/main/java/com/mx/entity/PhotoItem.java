package com.mx.entity;

import lombok.Data;

@Data
public class PhotoItem {

    private String id;
    private String relativePath;
    private String fileName;
    private String thumbnailUrl;
    private String status;
    private Long fileSize;
    private String dateTaken;
    private String cameraModel;
    private String fNumber;
    private String shutterSpeed;
    private String isoSpeed;
    private String flash;
    private String focalLength;
    private String lenInfo;

    public static final String STATUS_UNMARKED = "unmarked";
    public static final String STATUS_SELECTED = "selected";
    public static final String STATUS_REJECTED = "rejected";
}
