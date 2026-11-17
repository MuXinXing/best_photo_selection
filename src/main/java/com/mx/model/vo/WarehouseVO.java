package com.mx.model.vo;

import lombok.Data;

/** 片仓出参。 */
@Data
public class WarehouseVO {

    private String id;
    private String name;
    private String path;
    private boolean active;
}
