package com.mx.util;

import java.io.IOException;
import java.nio.file.FileAlreadyExistsException;
import java.nio.file.Files;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Stream;

/** 在片仓根内复制或删除精选文件，RAW 按主文件名全名匹配。 */
public final class WarehouseFiles {

    private static final Set<String> RAW_EXTENSIONS = Set.of(
            "cr2", "cr3", "crw",
            "nef", "nrw",
            "arw", "srf", "sr2",
            "raf",
            "rw2",
            "orf",
            "pef",
            "dng", "rwl",
            "x3f",
            "3fr", "fff",
            "dcr", "kdc",
            "mos",
            "stn",
            "mrw",
            "iiq",
            "srw"
    );

    private WarehouseFiles() {
    }

    public static boolean isRaw(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) {
            return false;
        }
        return RAW_EXTENSIONS.contains(fileName.substring(dot + 1).toLowerCase(Locale.ROOT));
    }

    public static boolean isJpg(String fileName) {
        String lower = fileName.toLowerCase(Locale.ROOT);
        return lower.endsWith(".jpg") || lower.endsWith(".jpeg");
    }

    public static String baseName(String fileName) {
        int dot = fileName.lastIndexOf('.');
        if (dot <= 0) {
            return fileName;
        }
        return fileName.substring(0, dot);
    }

    public static boolean sameBaseName(String fileName, String baseName) {
        return baseName.equals(baseName(fileName));
    }

    public static void copySelected(String activePath, String relativeJpgPath) throws IOException {
        Path scanRoot = WarehouseLayout.scanRoot(activePath);
        Path source = scanRoot.resolve(relativeJpgPath).normalize();
        if (!WarehouseLayout.isInside(scanRoot, source) || !isRegularInside(scanRoot, source)) {
            throw new IOException("源照片不在当前片仓内");
        }
        Path root = WarehouseLayout.rootPath(activePath);
        Path jpgDir = WarehouseLayout.ensureDirInside(root, root.resolve("选片").resolve("JPG"));
        Path target = jpgDir.resolve(source.getFileName().toString()).normalize();
        if (!WarehouseLayout.isInside(root, target)) {
            throw new IOException("选片目录超出片仓");
        }
        if (!Files.exists(target)) {
            Files.copy(source, target);
        }
        copyRawTwins(root, source.getFileName().toString());
    }

    public static void removeSelected(String activePath, String relativeJpgPath) throws IOException {
        Path scanRoot = WarehouseLayout.scanRoot(activePath);
        Path source = scanRoot.resolve(relativeJpgPath).normalize();
        String fileName = source.getFileName() == null ? "" : source.getFileName().toString();
        if (fileName.isEmpty()) {
            return;
        }
        Path root = WarehouseLayout.rootPath(activePath);
        deleteIfInside(root, root.resolve("选片").resolve("JPG").resolve(fileName));
        Path selectionRaw = root.resolve("选片").resolve("RAW");
        if (!Files.isDirectory(selectionRaw) || !WarehouseLayout.isInside(root, selectionRaw)) {
            return;
        }
        String baseName = baseName(fileName);
        List<Path> twins = listExactRaws(selectionRaw, baseName);
        for (Path twin : twins) {
            deleteIfInside(root, twin);
        }
    }

    private static void copyRawTwins(Path root, String jpgFileName) throws IOException {
        Path rawDir = root.resolve("RAW");
        if (!Files.isDirectory(rawDir) || !WarehouseLayout.isInside(root, rawDir)) {
            return;
        }
        List<Path> twins = listExactRaws(rawDir, baseName(jpgFileName));
        if (twins.isEmpty()) {
            return;
        }
        Path destDir = WarehouseLayout.ensureDirInside(root, root.resolve("选片").resolve("RAW"));
        for (Path raw : twins) {
            if (!isRegularInside(rawDir, raw)) {
                continue;
            }
            Path dest = destDir.resolve(raw.getFileName().toString()).normalize();
            if (!WarehouseLayout.isInside(root, dest) || Files.exists(dest)) {
                continue;
            }
            Files.copy(raw, dest);
        }
    }

    static List<Path> listExactRaws(Path rawDir, String baseName) throws IOException {
        List<Path> matched = new ArrayList<>();
        try (Stream<Path> files = Files.list(rawDir)) {
            for (Path file : files.toList()) {
                String name = file.getFileName().toString();
                if (sameBaseName(name, baseName) && isRaw(name)) {
                    matched.add(file);
                }
            }
        }
        return matched;
    }

    private static boolean isRegularInside(Path root, Path file) throws IOException {
        if (!Files.exists(file)) {
            return false;
        }
        if (Files.isSymbolicLink(file)) {
            Path real = file.toRealPath().normalize();
            Path rootReal = root.toRealPath().normalize();
            return real.startsWith(rootReal) && Files.isRegularFile(real);
        }
        return Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS);
    }

    private static void deleteIfInside(Path root, Path file) throws IOException {
        if (!Files.exists(file) || !WarehouseLayout.isInside(root, file)) {
            return;
        }
        if (Files.isSymbolicLink(file)) {
            Path parent = file.getParent();
            if (parent != null && WarehouseLayout.isInside(root, parent)) {
                Files.deleteIfExists(file);
            }
            return;
        }
        if (Files.isRegularFile(file, LinkOption.NOFOLLOW_LINKS)) {
            Files.deleteIfExists(file);
        }
    }

    /** 只移动。目标已存在时不覆盖，返回 false。 */
    public static boolean moveIfAbsent(Path source, Path target) throws IOException {
        if (Files.exists(target)) {
            return false;
        }
        try {
            Files.move(source, target);
            return true;
        } catch (FileAlreadyExistsException e) {
            return false;
        }
    }
}
