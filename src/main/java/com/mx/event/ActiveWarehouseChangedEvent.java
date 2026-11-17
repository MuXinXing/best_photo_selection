package com.mx.event;

/** 当前片仓或它的扫描路径变了，照片索引需要重建。 */
public record ActiveWarehouseChangedEvent(String warehouseId) {
}
