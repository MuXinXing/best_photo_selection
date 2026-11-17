package com.mx.service.impl;

import com.mx.event.ActiveWarehouseChangedEvent;
import com.mx.common.PhotoSelectionException;
import com.mx.model.bo.WarehouseBO;
import com.mx.service.IWarehouseService;
import com.mx.util.WarehouseLayout;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import jakarta.annotation.PostConstruct;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantLock;

@Service
@Log4j2
public class WarehouseServiceImpl implements IWarehouseService {

    /** 仓库配置文件存储在 ~/.photo-selection/warehouses.json */
    @Value("${app.config-dir}")
    private String configDir;

    private final ObjectMapper objectMapper = new ObjectMapper();
    private final ApplicationEventPublisher eventPublisher;
    private final ReentrantLock lock = new ReentrantLock();

    private final List<WarehouseBO> warehouses = new ArrayList<>();
    private WarehouseBO activeWarehouse;

    public WarehouseServiceImpl(ApplicationEventPublisher eventPublisher) {
        this.eventPublisher = eventPublisher;
    }

    @PostConstruct
    public void init() {
        loadWarehouses();
    }

    public List<WarehouseBO> listWarehouses() {
        lock.lock();
        try {
            return warehouses.stream().map(WarehouseBO::copy).toList();
        } finally {
            lock.unlock();
        }
    }

    public WarehouseBO getActiveWarehouse() {
        lock.lock();
        try {
            return activeWarehouse == null ? null : activeWarehouse.copy();
        } finally {
            lock.unlock();
        }
    }

    public WarehouseBO addWarehouse(String name, String path) {
        if (name == null || path == null) {
            throw new IllegalArgumentException("仓库名称和路径不能为空");
        }
        String trimmedName = name.trim();
        String normalizedPath = WarehouseLayout.normalize(path);
        if (trimmedName.isEmpty() || normalizedPath.isEmpty()) {
            throw new IllegalArgumentException("仓库名称和路径不能为空");
        }
        WarehouseBO created = new WarehouseBO();
        created.setId(UUID.randomUUID().toString());
        created.setName(trimmedName);
        created.setPath(normalizedPath);
        boolean publish = false;
        lock.lock();
        List<WarehouseBO> saved = copyWarehouses();
        String previousActiveId = activeId();
        try {
            created.setActive(warehouses.isEmpty());
            warehouses.add(created);
            if (created.isActive()) {
                activeWarehouse = created;
                publish = true;
            }
            saveWarehouses();
        } catch (RuntimeException e) {
            restore(saved, previousActiveId);
            throw e;
        } finally {
            lock.unlock();
        }
        if (publish) {
            publishActive(created.getId());
        }
        log.info("已添加片仓: {} -> {}", trimmedName, normalizedPath);
        return created.copy();
    }

    public void setActive(String id) {
        String activeId;
        lock.lock();
        List<WarehouseBO> saved = copyWarehouses();
        String previousActiveId = activeId();
        try {
            WarehouseBO target = find(id);
            if (target == null) {
                throw new PhotoSelectionException("仓库不存在");
            }
            for (WarehouseBO wh : warehouses) {
                wh.setActive(wh.getId().equals(id));
            }
            activeWarehouse = target;
            saveWarehouses();
            activeId = target.getId();
        } catch (RuntimeException e) {
            restore(saved, previousActiveId);
            throw e;
        } finally {
            lock.unlock();
        }
        publishActive(activeId);
        log.info("当前片仓已切换为: {}", activeId);
    }

    public void removeWarehouse(String id) {
        String activeId;
        lock.lock();
        List<WarehouseBO> saved = copyWarehouses();
        String previousActiveId = activeId();
        try {
            boolean removed = warehouses.removeIf(wh -> wh.getId().equals(id));
            if (!removed) {
                throw new PhotoSelectionException("仓库不存在");
            }
            if (activeWarehouse != null && activeWarehouse.getId().equals(id)) {
                activeWarehouse = null;
            }
            if (activeWarehouse == null && !warehouses.isEmpty()) {
                warehouses.get(0).setActive(true);
                activeWarehouse = warehouses.get(0);
            }
            for (WarehouseBO wh : warehouses) {
                wh.setActive(activeWarehouse != null && wh.getId().equals(activeWarehouse.getId()));
            }
            saveWarehouses();
            activeId = activeWarehouse == null ? "" : activeWarehouse.getId();
        } catch (RuntimeException e) {
            restore(saved, previousActiveId);
            throw e;
        } finally {
            lock.unlock();
        }
        publishActive(activeId);
        log.info("已删除片仓: {}", id);
    }

    /** 分类完成后把指定仓库的扫描路径改成 JPG/ 并落盘。只有它仍是活动仓时才重建索引。 */
    public void updateWarehousePath(String id, String path) {
        boolean publish = false;
        String activeId = "";
        lock.lock();
        List<WarehouseBO> saved = copyWarehouses();
        String previousActiveId = activeId();
        try {
            WarehouseBO target = find(id);
            if (target == null) {
                log.warn("片仓已不存在，跳过路径更新: {}", id);
                return;
            }
            target.setPath(WarehouseLayout.normalize(path));
            saveWarehouses();
            if (target.isActive()) {
                publish = true;
                activeId = target.getId();
            }
        } catch (RuntimeException e) {
            restore(saved, previousActiveId);
            throw e;
        } finally {
            lock.unlock();
        }
        if (publish) {
            publishActive(activeId);
        }
        log.info("片仓 {} 的路径已更新为 {}", id, path);
    }

    private String activeId() {
        return activeWarehouse == null ? null : activeWarehouse.getId();
    }

    private List<WarehouseBO> copyWarehouses() {
        return warehouses.stream().map(WarehouseBO::copy).toList();
    }

    private void restore(List<WarehouseBO> saved, String activeId) {
        warehouses.clear();
        for (WarehouseBO wh : saved) {
            warehouses.add(wh.copy());
        }
        activeWarehouse = activeId == null ? null : find(activeId);
    }

    private WarehouseBO find(String id) {
        for (WarehouseBO wh : warehouses) {
            if (wh.getId().equals(id)) {
                return wh;
            }
        }
        return null;
    }

    private void publishActive(String warehouseId) {
        eventPublisher.publishEvent(new ActiveWarehouseChangedEvent(warehouseId));
    }

    private void loadWarehouses() {
        lock.lock();
        try {
            File configFile = getConfigFile();
            if (!configFile.exists()) {
                log.info("未找到片仓配置，从空白开始");
                return;
            }
            List<WarehouseBO> loaded = objectMapper.readValue(configFile, new TypeReference<List<WarehouseBO>>() {});
            warehouses.clear();
            WarehouseBO active = null;
            for (WarehouseBO wh : loaded) {
                if (wh.isActive() && active == null) {
                    active = wh;
                } else {
                    wh.setActive(false);
                }
                warehouses.add(wh);
            }
            activeWarehouse = active;
            log.info("已加载 {} 个片仓，当前片仓: {}", warehouses.size(),
                    activeWarehouse != null ? activeWarehouse.getName() : "none");
        } catch (Exception e) {
            log.error("读取片仓配置失败", e);
        } finally {
            lock.unlock();
        }
    }

    private void saveWarehouses() {
        try {
            File configFile = getConfigFile();
            Path parent = configFile.toPath().getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }
            Path tmp = configFile.toPath().resolveSibling(configFile.getName() + ".tmp");
            objectMapper.writeValue(tmp.toFile(), warehouses);
            Files.move(tmp, configFile.toPath(), java.nio.file.StandardCopyOption.REPLACE_EXISTING);
        } catch (Exception e) {
            log.error("保存片仓配置失败", e);
            throw new PhotoSelectionException("保存仓库配置失败", 500, e);
        }
    }

    private File getConfigFile() {
        return Paths.get(configDir, "warehouses.json").toFile();
    }
}
