package com.mx.entity;

import lombok.Data;

/**
 * @author MuXin
 * @create By MX-Desktop 2026/3/25 15:46
 * @system Windows 10 21H2 专业工作站版
 * @Gitee https://gitee.com/mxgitstorage
 */

@Data
public class PhotoExif {

    private String makerNote;
    private String fNumber;
    private String shutterSpeed;
    private String isoSpeed;
    private String flash;
    private String focalLength;
    private String lenInfo;
}
