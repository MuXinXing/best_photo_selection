package com.mx.model.vo;

import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/** 批量标记结果。 */
@Data
public class BatchMarkVO {

    private int updated;
    private List<String> failed = new ArrayList<>();
}
