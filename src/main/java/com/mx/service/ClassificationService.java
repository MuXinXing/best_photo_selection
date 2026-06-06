package com.mx.service;

import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Service;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Stream;

/**
 * 照片分类服务：将仓库根目录的 JPG/RAW 移动到对应子文件夹，
 * 并支持将精选照片复制到"选片"目录。
 */
@Service
@Log4j2
public class ClassificationService {

    /** 所有已知 RAW 文件后缀（大小写不敏感） */
    private static final Set<String> RAW_EXTENSIONS = new HashSet<>(Arrays.asList(
            "cr2", "cr3", "crw",       // 佳能 Canon
            "nef", "nrw",               // 尼康 Nikon
            "arw", "srf", "sr2",        // 索尼 Sony
            "raf",                      // 富士 Fujifilm
            "rw2",                      // 松下 Panasonic
            "orf",                      // 奥林巴斯 Olympus
            "pef",                      // 宾得 Pentax
            "dng", "rwl",               // 徕卡 Leica / Adobe
            "x3f",                      // 适马 Sigma
            "3fr", "fff",               // 哈苏 Hasselblad
            "dcr", "kdc",               // 柯达 Kodak
            "mos",                      // 玛米亚 Leaf
            "stn",                      // 仙娜 Sinar
            "mrw",                      // 美能达 Minolta
            "iiq",                      // 飞思 Phase One
            "srw"                       // 三星 Samsung
    ));

    private final AtomicInteger totalFiles = new AtomicInteger(0);
    private final AtomicInteger processedFiles = new AtomicInteger(0);
    private volatile String currentPhase = "";
    private volatile boolean classifying = false;

    /** 判断文件是否为 RAW */
    public static boolean isRaw(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0) return false;
        return RAW_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase());
    }

    /** 判断文件是否为 JPG */
    public static boolean isJpg(String fileName) {
        String lower = fileName.toLowerCase();
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    /** 获取进度百分比（0~100） */
    public int getProgress() {
        int total = totalFiles.get();
        if (total == 0) return 100;
        return processedFiles.get() * 100 / total;
    }

    /** 获取当前阶段描述 */
    public String getCurrentPhase() {
        return currentPhase;
    }

    /** 是否正在分类中 */
    public boolean isClassifying() {
        return classifying;
    }

    /**
     * 同步执行分类：扫描仓库根目录，将 JPG 和 RAW 分别移动到对应子文件夹。
     * 完成后 warehouseService 的路径自动切换为 JPG/。
     */
    public void classify(String warehousePath, WarehouseService warehouseService) {
        classifying = true;
        processedFiles.set(0);
        File root = new File(warehousePath);

        // 统计待处理的文件数
        currentPhase = "正在扫描文件...";
        try (Stream<Path> allFiles = Files.walk(root.toPath(), 1)) {
            long count = allFiles
                    .filter(Files::isRegularFile)
                    .filter(p -> {
                        String name = p.getFileName().toString().toLowerCase();
                        return name.endsWith(".jpg") || name.endsWith(".jpeg") || isRaw(p.getFileName().toString());
                    })
                    .count();
            totalFiles.set((int) count);
        } catch (IOException e) {
            log.error("Failed to scan root directory: {}", e.getMessage());
            totalFiles.set(0);
        }

        if (totalFiles.get() == 0) {
            classifying = false;
            currentPhase = "无需分类";
            return;
        }

        // 创建目标目录
        File jpgDir = new File(root, "JPG");
        File rawDir = new File(root, "RAW");
        File selectionJpgDir = new File(root, "选片/JPG");
        File selectionRawDir = new File(root, "选片/RAW");
        jpgDir.mkdirs();
        rawDir.mkdirs();
        selectionJpgDir.mkdirs();
        selectionRawDir.mkdirs();

        // 遍历根目录（仅第一层）并移动文件
        try (Stream<Path> files = Files.list(root.toPath())) {
            files.filter(Files::isRegularFile).forEach(p -> {
                String name = p.getFileName().toString();
                try {
                    if (isJpg(name)) {
                        Files.move(p, new File(jpgDir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    } else if (isRaw(name)) {
                        Files.move(p, new File(rawDir, name).toPath(), StandardCopyOption.REPLACE_EXISTING);
                    }
                } catch (IOException e) {
                    log.warn("Failed to move {}: {}", name, e.getMessage());
                }
                processedFiles.incrementAndGet();
            });
        } catch (IOException e) {
            log.error("Failed to list root directory: {}", e.getMessage());
        }

        log.info("Classification complete: {} files processed", processedFiles.get());

        // 更新仓库路径为 JPG/ 子目录
        String jpgPath = warehousePath.replace("\\", "/") + "/JPG";
        if (!warehouseService.listWarehouses().isEmpty()) {
            warehouseService.getActiveWarehouse().setPath(jpgPath);
        }

        classifying = false;
        currentPhase = "分类完成";
    }

    /**
     * 将照片复制到"选片"目录。
     * 同时复制对应的 RAW 文件（如果存在）。
     */
    public static void copyToSelection(String warehousePath, String relativeJpgPath) {
        String jpgFullPath = warehousePath + "/" + relativeJpgPath;
        File jpgFile = new File(jpgFullPath);
        if (!jpgFile.exists()) return;

        // 复制 JPG 到 选片/JPG
        File targetJpgDir = new File(new File(warehousePath).getParent(), "选片/JPG");
        targetJpgDir.mkdirs();
        File targetJpg = new File(targetJpgDir, jpgFile.getName());
        try {
            Files.copy(jpgFile.toPath(), targetJpg.toPath(), StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException e) {
            log.warn("Failed to copy JPG to selection: {}", e.getMessage());
        }

        // 尝试找对应的 RAW 文件并复制到 选片/RAW
        String baseName = jpgFile.getName().substring(0, jpgFile.getName().lastIndexOf('.'));
        File rawDir = new File(new File(warehousePath).getParent(), "RAW");
        if (rawDir.exists()) {
            File[] rawFiles = rawDir.listFiles((dir, name) -> {
                if (name.startsWith(baseName)) {
                    return isRaw(name);
                }
                return false;
            });
            if (rawFiles != null) {
                File targetRawDir = new File(new File(warehousePath).getParent(), "选片/RAW");
                targetRawDir.mkdirs();
                for (File rawFile : rawFiles) {
                    try {
                        Files.copy(rawFile.toPath(), new File(targetRawDir, rawFile.getName()).toPath(),
                                StandardCopyOption.REPLACE_EXISTING);
                    } catch (IOException e) {
                        log.warn("Failed to copy RAW to selection: {}", e.getMessage());
                    }
                }
            }
        }
    }

    /**
     * 从"选片"目录中移除照片（当精选被重置时调用）。
     * 同时删除对应的 JPG 和 RAW 文件。
     */
    public static void removeFromSelection(String warehousePath, String relativeJpgPath) {
        String jpgFileName = relativeJpgPath.contains("/")
                ? relativeJpgPath.substring(relativeJpgPath.lastIndexOf('/') + 1)
                : relativeJpgPath;
        String root = warehousePath.replace("\\", "/");
        if (root.endsWith("/JPG")) root = root.substring(0, root.length() - 4);

        // 删除 选片/JPG/xxx.jpg
        File jpgFile = new File(root + "/选片/JPG/" + jpgFileName);
        if (jpgFile.exists() && jpgFile.delete()) {
            log.debug("Removed from selection JPG: {}", jpgFileName);
        }

        // 删除 选片/RAW/xxx.*（同文件名不同后缀的 RAW）
        String baseName = jpgFileName.contains(".")
                ? jpgFileName.substring(0, jpgFileName.lastIndexOf('.'))
                : jpgFileName;
        File selectionRawDir = new File(root + "/选片/RAW");
        if (selectionRawDir.exists()) {
            File[] matched = selectionRawDir.listFiles((dir, name) -> name.startsWith(baseName + "."));
            if (matched != null) {
                for (File f : matched) {
                    if (f.delete()) {
                        log.debug("Removed from selection RAW: {}", f.getName());
                    }
                }
            }
        }
    }
}
