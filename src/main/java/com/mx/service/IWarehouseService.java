package com.mx.service;

import com.mx.model.bo.WarehouseBO;

import java.util.List;

/** 片仓配置的读写。不包含选片业务。 */
public interface IWarehouseService {

    List<WarehouseBO> listWarehouses();

    WarehouseBO getActiveWarehouse();

    WarehouseBO addWarehouse(String name, String path);

    void setActive(String id);

    void removeWarehouse(String id);

    void updateWarehousePath(String id, String path);
}
