package com.mx.controller;

import com.mx.service.WarehouseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouse")
@Log4j2
public class WarehouseController {

    @Autowired
    private WarehouseService warehouseService;

    /** 获取所有已配置的仓库列表 */
    @GetMapping
    public ResponseEntity<List<WarehouseService.Warehouse>> list() {
        return ResponseEntity.ok(warehouseService.listWarehouses());
    }

    /** 新增仓库 */
    @PostMapping
    public ResponseEntity<WarehouseService.Warehouse> add(@RequestBody Map<String, String> body) {
        String name = body.get("name");
        String path = body.get("path");
        if (name == null || path == null) {
            return ResponseEntity.badRequest().build();
        }
        WarehouseService.Warehouse wh = warehouseService.addWarehouse(name, path);
        return ResponseEntity.ok(wh);
    }

    /** 切换当前仓库 */
    @PutMapping("/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable("id") String id) {
        warehouseService.setActive(id);
        return ResponseEntity.ok().build();
    }

    /** 删除仓库 */
    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") String id) {
        warehouseService.removeWarehouse(id);
        return ResponseEntity.ok().build();
    }
}
