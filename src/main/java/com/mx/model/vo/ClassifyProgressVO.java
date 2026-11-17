package com.mx.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 文件整理进度。 */
@Data
public class ClassifyProgressVO {

    private boolean classifying;
    private int progress;
    private String phase;
    private List<String> skipped = new ArrayList<>();
}
