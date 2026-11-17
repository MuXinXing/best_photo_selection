package com.mx.controller;

import com.mx.common.ApiResponse;
import com.mx.logic.WarehouseLogic;
import com.mx.model.dto.WarehouseSaveDTO;
import com.mx.model.vo.WarehouseVO;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/** 片仓接口。只负责组参和返回，业务在 WarehouseLogic。 */
@RestController
@RequestMapping("/api/warehouse")
public class WarehouseController {

    private final WarehouseLogic warehouseLogic;

    public WarehouseController(WarehouseLogic warehouseLogic) {
        this.warehouseLogic = warehouseLogic;
    }

    @GetMapping
    public ApiResponse<List<WarehouseVO>> list() {
        return ApiResponse.ok(warehouseLogic.list());
    }

    @PostMapping
    public ApiResponse<WarehouseVO> add(@RequestBody WarehouseSaveDTO body) {
        return ApiResponse.ok(warehouseLogic.add(body));
    }

    @PutMapping("/{id}/active")
    public ApiResponse<Void> setActive(@PathVariable("id") String id) {
        warehouseLogic.setActive(id);
        return ApiResponse.ok(null);
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> remove(@PathVariable("id") String id) {
        warehouseLogic.remove(id);
        return ApiResponse.ok(null);
    }
}
