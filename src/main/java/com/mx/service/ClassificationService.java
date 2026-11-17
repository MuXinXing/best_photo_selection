package com.mx.service;

import com.mx.util.WarehouseFiles;
import com.mx.util.WarehouseLayout;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 把片仓根目录的 JPG/RAW 移到子目录。同一时间只允许一个分类任务。
 * 目标已存在时跳过，不覆盖。
 */
@Service
@Log4j2
public class ClassificationService {

    private final WarehouseService warehouseService;
    private final ExecutorService classificationExecutor;

    private final AtomicBoolean classifying = new AtomicBoolean(false);
    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private final List<String> skipped = Collections.synchronizedList(new ArrayList<>());
    private volatile String currentPhase = "";

    public ClassificationService(WarehouseService warehouseService,
                                 @Qualifier("classificationExecutor") ExecutorService classificationExecutor) {
        this.warehouseService = warehouseService;
        this.classificationExecutor = classificationExecutor;
    }

    public boolean isClassifying() {
        return classifying.get();
    }

    public int getProgress() {
        int total = totalFiles.get();
        int processed = processedFiles.get();
        if (total <= 0) {
            return classifying.get() ? 0 : 100;
        }
        return Math.min(100, processed * 100 / total);
    }

    public String getCurrentPhase() {
        return currentPhase;
    }

    public List<String> getSkipped() {
        synchronized (skipped) {
            return List.copyOf(skipped);
        }
    }

    public boolean start(String warehouseId, String warehousePath) {
        if (!classifying.compareAndSet(false, true)) {
            return false;
        }
        processedFiles.set(0);
        totalFiles.set(0);
        skipped.clear();
        currentPhase = "正在准备整理...";
        try {
            classificationExecutor.execute(() -> {
                try {
                    classify(warehouseId, warehousePath);
                } catch (Exception e) {
                    log.error("分类失败", e);
                    currentPhase = "分类失败";
                } finally {
                    classifying.set(false);
                }
            });
        } catch (RuntimeException e) {
            classifying.set(false);
            throw e;
        }
        return true;
    }

    private void classify(String warehouseId, String warehousePath) throws IOException {
        String normalized = WarehouseLayout.normalize(warehousePath);
        if (WarehouseLayout.isJpgFolder(normalized)) {
            warehouseService.updateWarehousePath(warehouseId, normalized);
            currentPhase = "分类完成";
            return;
        }
        Path root = Paths.get(normalized);
        if (!Files.isDirectory(root)) {
            currentPhase = "分类失败";
            log.warn("分类目录不存在: {}", normalized);
            return;
        }
        Path rootReal = root.toRealPath().normalize();
        Path jpgDir = root.resolve("JPG");
        if (Files.isDirectory(jpgDir) && WarehouseLayout.isInside(rootReal, jpgDir) && directoryHasEntries(jpgDir)) {
            Path jpgReal = jpgDir.toRealPath().normalize();
            if (!jpgReal.startsWith(rootReal)) {
                currentPhase = "分类失败";
                log.warn("JPG 目录指向片仓外: {}", jpgDir);
                return;
            }
            warehouseService.updateWarehousePath(warehouseId, WarehouseLayout.normalize(jpgReal.toString()));
            currentPhase = "分类完成";
            return;
        }

        currentPhase = "正在扫描文件...";
        List<Path> targets = new ArrayList<>();
        try (Stream<Path> files = Files.list(root)) {
            for (Path file : files.toList()) {
                if (shouldMove(rootReal, file)) {
                    targets.add(file);
                }
            }
        }
        totalFiles.set(targets.size());
        if (targets.isEmpty()) {
            currentPhase = "无需分类";
            return;
        }

        currentPhase = "正在移动文件...";
        Path jpgTargetDir = WarehouseLayout.ensureDirInside(rootReal, root.resolve("JPG"));
        Path rawTargetDir = WarehouseLayout.ensureDirInside(rootReal, root.resolve("RAW"));
        WarehouseLayout.ensureDirInside(rootReal, root.resolve("选片").resolve("JPG"));
        WarehouseLayout.ensureDirInside(rootReal, root.resolve("选片").resolve("RAW"));

        for (Path file : targets) {
            String name = file.getFileName().toString();
            Path destDir = WarehouseFiles.isJpg(name) ? jpgTargetDir : rawTargetDir;
            Path dest = destDir.resolve(name).normalize();
            try {
                if (!WarehouseLayout.isInside(rootReal, dest)) {
                    skip(name, "目标超出片仓");
                } else if (!WarehouseFiles.moveIfAbsent(file, dest)) {
                    skip(name, "目标已存在");
                }
            } catch (IOException e) {
                skip(name, e.getMessage());
            }
            processedFiles.incrementAndGet();
        }

        warehouseService.updateWarehousePath(warehouseId, WarehouseLayout.normalize(jpgTargetDir.toString()));
        currentPhase = skipped.isEmpty() ? "分类完成" : "分类完成，跳过 " + skipped.size() + " 个文件";
        log.info("Classification complete: processed={}, skipped={}", processedFiles.get(), skipped.size());
    }

    private void skip(String name, String reason) {
        skipped.add(name);
        log.warn("跳过 {}: {}", name, reason);
    }

    private static boolean directoryHasEntries(Path dir) throws IOException {
        try (Stream<Path> entries = Files.list(dir)) {
            return entries.findAny().isPresent();
        }
    }

    private static boolean shouldMove(Path rootReal, Path file) {
        try {
            String name = file.getFileName().toString();
            if (!WarehouseFiles.isJpg(name) && !WarehouseFiles.isRaw(name)) {
                return false;
            }
            if (Files.isSymbolicLink(file)) {
                Path real = file.toRealPath().normalize();
                return real.startsWith(rootReal) && Files.isRegularFile(real);
            }
            return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
        } catch (IOException e) {
            return false;
        }
    }
}
