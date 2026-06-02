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

    @GetMapping
    public ResponseEntity<List<WarehouseService.Warehouse>> list() {
        return ResponseEntity.ok(warehouseService.listWarehouses());
    }

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
