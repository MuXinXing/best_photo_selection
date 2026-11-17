package com.mx.model.bo;

import lombok.Data;

/** 片仓配置，保存在本地 JSON，不直接作为接口出参。 */
@Data
public class WarehouseBO {

    private String id;
    private String name;
    private String path;
    private boolean active;

    public WarehouseBO copy() {
        WarehouseBO copy = new WarehouseBO();
        copy.id = id;
        copy.name = name;
        copy.path = path;
        copy.active = active;
        return copy;
    }
}
