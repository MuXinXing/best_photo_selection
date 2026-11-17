package com.mx.model.vo;

import lombok.Data;

/** 照片出参。不包含磁盘相对路径等内部字段。 */
@Data
public class PhotoVO {

    private String id;
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
}
