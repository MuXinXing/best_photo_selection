package com.mx.service;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.Data;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@Log4j2
public class WarehouseService {

    /** 仓库配置文件存储在 ~/.photo-selection/warehouses.json */
    @Value("${app.config-dir}")
    private String configDir;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private final List<Warehouse> warehouses = new ArrayList<>();
    private Warehouse activeWarehouse;

    @PostConstruct
    public void init() {
        loadWarehouses();
    }

    /** 获取所有已配置的仓库 */
    public List<Warehouse> listWarehouses() {
        return warehouses;
    }

    /** 获取当前活动的仓库 */
    public Warehouse getActiveWarehouse() {
        return activeWarehouse;
    }

    /** 添加新仓库，第一个添加的会自动设为当前仓库 */
    public Warehouse addWarehouse(String name, String path) {
        Warehouse wh = new Warehouse();
        wh.setId(UUID.randomUUID().toString());
        wh.setName(name);
        wh.setPath(normalizePath(path));
        wh.setActive(warehouses.isEmpty());
        warehouses.add(wh);
        if (wh.isActive()) {
            activeWarehouse = wh;
        }
        saveWarehouses();
        log.info("Added warehouse: {} -> {}", name, path);
        return wh;
    }

    /** 切换当前仓库 */
    public void setActive(String id) {
        for (Warehouse wh : warehouses) {
            wh.setActive(wh.getId().equals(id));
            if (wh.isActive()) {
                activeWarehouse = wh;
            }
        }
        saveWarehouses();
        log.info("Active warehouse switched to: {}", activeWarehouse != null ? activeWarehouse.getName() : "none");
    }

    /** 删除仓库，如果删除的是当前仓库则自动切换到第一个 */
    public void removeWarehouse(String id) {
        warehouses.removeIf(wh -> {
            if (wh.getId().equals(id)) {
                if (wh.isActive()) {
                    activeWarehouse = null;
                }
                return true;
            }
            return false;
        });
        if (activeWarehouse == null && !warehouses.isEmpty()) {
            warehouses.get(0).setActive(true);
            activeWarehouse = warehouses.get(0);
        }
        saveWarehouses();
        log.info("Removed warehouse: {}", id);
    }

    /** 从 JSON 文件加载仓库配置 */
    private void loadWarehouses() {
        try {
            File configFile = getConfigFile();
            if (configFile.exists()) {
                List<Warehouse> loaded = objectMapper.readValue(configFile, new TypeReference<List<Warehouse>>() {});
                warehouses.addAll(loaded);
                activeWarehouse = warehouses.stream().filter(Warehouse::isActive).findFirst().orElse(null);
                log.info("Loaded {} warehouses, active: {}", warehouses.size(), activeWarehouse != null ? activeWarehouse.getName() : "none");
            } else {
                log.info("No warehouse config found, starting empty");
            }
        } catch (Exception e) {
            log.error("Failed to load warehouses config", e);
        }
    }

    /** 将仓库配置写入 JSON 文件 */
    private void saveWarehouses() {
        try {
            File configFile = getConfigFile();
            configFile.getParentFile().mkdirs();
            objectMapper.writeValue(configFile, warehouses);
        } catch (Exception e) {
            log.error("Failed to save warehouses config", e);
        }
    }

    private File getConfigFile() {
        return Paths.get(configDir, "warehouses.json").toFile();
    }

    /** 统一路径格式为 Unix 风格，去除尾部斜杠 */
    private String normalizePath(String path) {
        return path.replace("\\", "/").replaceAll("/+$", "");
    }

    /** 仓库配置数据模型 */
    @Data
    public static class Warehouse {
        private String id;
        private String name;
        private String path;
        private boolean active;
    }
}
