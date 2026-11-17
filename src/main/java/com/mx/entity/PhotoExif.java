package com.mx.entity;

import lombok.Data;

/** EXIF 元数据（从 JPG 中提取的拍摄参数） */
@Data
public class PhotoExif {

    /** 相机型号，来自 IFD0 TAG_MODEL */
    private String cameraModel;
    /** 光圈值，如 f/2.8 */
    private String fNumber;
    /** 快门速度，如 1/250 */
    private String shutterSpeed;
    /** ISO 感光度 */
    private String isoSpeed;
    /** 闪光灯触发状态 */
    private String flash;
    /** 焦距，如 50mm */
    private String focalLength;
    /** 镜头型号 */
    private String lenInfo;
    /** 原始拍摄时间 */
    private String dateTaken;
}
