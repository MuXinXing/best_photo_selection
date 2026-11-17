package com.mx.controller;

import com.mx.service.WarehouseService;
import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/warehouse")
@Log4j2
public class WarehouseController {

    private final WarehouseService warehouseService;

    public WarehouseController(WarehouseService warehouseService) {
        this.warehouseService = warehouseService;
    }

    @GetMapping
    public ResponseEntity<List<WarehouseService.Warehouse>> list() {
        return ResponseEntity.ok(warehouseService.listWarehouses());
    }

    @PostMapping
    public ResponseEntity<WarehouseService.Warehouse> add(@RequestBody Map<String, String> body) {
        if (body == null) {
            throw new IllegalArgumentException("请求体不能为空");
        }
        return ResponseEntity.ok(warehouseService.addWarehouse(body.get("name"), body.get("path")));
    }

    @PutMapping("/{id}/active")
    public ResponseEntity<Void> setActive(@PathVariable("id") String id) {
        warehouseService.setActive(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> remove(@PathVariable("id") String id) {
        warehouseService.removeWarehouse(id);
        return ResponseEntity.ok().build();
    }
}
