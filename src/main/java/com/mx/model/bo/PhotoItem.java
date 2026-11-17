package com.mx.model.bo;

import lombok.Data;

/** 进程内照片对象，不直接返回给接口。 */
@Data
public class PhotoItem {

    /** Base64 编码的唯一 ID（基于相对路径生成） */
    private String id;
    /** 相对于仓库根目录的路径 */
    private String relativePath;
    /** 文件名（含扩展名） */
    private String fileName;
    /** 缩略图/原图访问 URL */
    private String thumbnailUrl;
    /** 标记状态: unmarked / selected / rejected */
    private String status;
    /** 文件大小（字节） */
    private Long fileSize;
    /** EXIF 拍摄时间 */
    private String dateTaken;
    /** 相机型号 */
    private String cameraModel;
    /** 光圈值 */
    private String fNumber;
    /** 快门速度 */
    private String shutterSpeed;
    /** ISO 感光度 */
    private String isoSpeed;
    /** 闪光灯状态 */
    private String flash;
    /** 焦距 */
    private String focalLength;
    /** 镜头信息 */
    private String lenInfo;

    public static final String STATUS_UNMARKED = "unmarked";
    public static final String STATUS_SELECTED = "selected";
    public static final String STATUS_REJECTED = "rejected";

    public static boolean isValidStatus(String status) {
        return STATUS_UNMARKED.equals(status) || STATUS_SELECTED.equals(status) || STATUS_REJECTED.equals(status);
    }
}
