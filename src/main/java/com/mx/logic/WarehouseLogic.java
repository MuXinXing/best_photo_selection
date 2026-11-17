package com.mx.logic;

import com.mx.model.bo.WarehouseBO;
import com.mx.model.dto.WarehouseSaveDTO;
import com.mx.model.vo.WarehouseVO;
import com.mx.service.IWarehouseService;
import org.springframework.stereotype.Component;

import java.util.List;

/** 片仓配置的业务入口。 */
@Component
public class WarehouseLogic {

    private final IWarehouseService warehouseService;

    public WarehouseLogic(IWarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    public List<WarehouseVO> list() {
        return warehouseService.listWarehouses().stream().map(this::toVo).toList();
    }

    public WarehouseVO add(WarehouseSaveDTO dto) {
        if (dto == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        return toVo(warehouseService.addWarehouse(dto.getName(), dto.getPath()));
    }

    public void setActive(String id) {
        warehouseService.setActive(id);
    }

    public void remove(String id) {
        warehouseService.removeWarehouse(id);
    }

    private WarehouseVO toVo(WarehouseBO source) {
        WarehouseVO vo = new WarehouseVO();
        vo.setId(source.getId());
        vo.setName(source.getName());
        vo.setPath(source.getPath());
        vo.setActive(source.isActive());
        return vo;
    }
}
