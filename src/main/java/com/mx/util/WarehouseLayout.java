package com.mx.util;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

/** 片仓根、扫描目录和选片目录的路径规则。 */
public final class WarehouseLayout {

    private static final String JPG_DIR = "/JPG";

    private WarehouseLayout() {
    }

    public static String normalize(String path) {
        if (path == null) {
            return "";
        }
        return path.trim().replace('\\', '/').replaceAll("/+$", "");
    }

    public static boolean isJpgFolder(String path) {
        return normalize(path).endsWith(JPG_DIR);
    }

    /** 活动路径以 /JPG 结尾时，片仓根是它的上一级；否则活动路径就是片仓根。 */
    public static String warehouseRoot(String activePath) {
        String normalized = normalize(activePath);
        if (normalized.endsWith(JPG_DIR)) {
            String root = normalized.substring(0, normalized.length() - JPG_DIR.length());
            return root.isEmpty() ? "/" : root;
        }
        return normalized;
    }

    public static int clampThumbnailWidth(int width) {
        if (width < 200) {
            return 200;
        }
        if (width > 1600) {
            return 1600;
        }
        return width;
    }

    /**
     * 真实路径必须落在 root 之内。候选文件还不存在时，检查其父目录的真实路径。
     */
    public static boolean isInside(Path root, Path candidate) {
        try {
            if (root == null || candidate == null || !Files.exists(root)) {
                return false;
            }
            Path rootReal = root.toRealPath().normalize();
            Path check;
            if (Files.exists(candidate)) {
                check = candidate.toRealPath().normalize();
            } else {
                Path parent = candidate.getParent();
                if (parent == null || !Files.exists(parent)) {
                    return false;
                }
                check = parent.toRealPath().normalize().resolve(candidate.getFileName()).normalize();
            }
            return check.startsWith(rootReal);
        } catch (IOException e) {
            return false;
        }
    }

    /** 逐级创建目录，任一层的真实路径离开片仓根则失败。 */
    public static Path ensureDirInside(Path root, Path dir) throws IOException {
        if (!Files.exists(root)) {
            throw new IOException("片仓根目录不存在");
        }
        Path rootReal = root.toRealPath().normalize();
        return mkdirInside(rootReal, dir.normalize());
    }

    private static Path mkdirInside(Path rootReal, Path dir) throws IOException {
        if (Files.exists(dir)) {
            Path real = dir.toRealPath().normalize();
            if (!real.startsWith(rootReal)) {
                throw new IOException("目录超出片仓: " + dir);
            }
            return real;
        }
        Path parent = dir.getParent();
        if (parent == null) {
            throw new IOException("目录超出片仓: " + dir);
        }
        Path parentReal = mkdirInside(rootReal, parent);
        Path created = parentReal.resolve(dir.getFileName()).normalize();
        if (!created.startsWith(rootReal)) {
            throw new IOException("目录超出片仓: " + dir);
        }
        Files.createDirectories(created);
        return created.toRealPath().normalize();
    }

    public static Path scanRoot(String activePath) {
        return Paths.get(normalize(activePath));
    }

    public static Path rootPath(String activePath) {
        return Paths.get(warehouseRoot(activePath));
    }
}
