package com.mx.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class WarehouseFilesTest {

    @TempDir
    Path temp;

    @Test
    void rawMatchIsExactBaseName() throws Exception {
        Path rawDir = temp.resolve("RAW");
        Files.createDirectories(rawDir);
        Files.writeString(rawDir.resolve("IMG_1.CR2"), "one");
        Files.writeString(rawDir.resolve("IMG_10.CR2"), "ten");
        Files.writeString(rawDir.resolve("IMG_1.NEF"), "nef");
        Files.writeString(rawDir.resolve("IMG_1.txt"), "text");

        List<Path> twins = WarehouseFiles.listExactRaws(rawDir, "IMG_1");
        assertEquals(2, twins.size());
        assertTrue(twins.stream().anyMatch(path -> path.getFileName().toString().equals("IMG_1.CR2")));
        assertTrue(twins.stream().anyMatch(path -> path.getFileName().toString().equals("IMG_1.NEF")));
    }

    @Test
    void copyAndRemoveStayInsideWarehouseAndDoNotOverwrite() throws Exception {
        Path root = temp.resolve("bank");
        writeJpeg(root.resolve("IMG_1.JPG"), 0x112233);
        Files.createDirectories(root.resolve("RAW"));
        Files.writeString(root.resolve("RAW").resolve("IMG_1.CR2"), "raw-1");
        Files.writeString(root.resolve("RAW").resolve("IMG_10.CR2"), "raw-10");
        Path selectedJpg = root.resolve("选片").resolve("JPG").resolve("IMG_1.JPG");
        Files.createDirectories(selectedJpg.getParent());
        writeJpeg(selectedJpg, 0xabcdef);
        byte[] existing = Files.readAllBytes(selectedJpg);

        WarehouseFiles.copySelected(root.toString(), "IMG_1.JPG");

        assertTrue(sameBytes(existing, Files.readAllBytes(selectedJpg)));
        assertTrue(Files.exists(root.resolve("选片").resolve("RAW").resolve("IMG_1.CR2")));
        assertFalse(Files.exists(root.resolve("选片").resolve("RAW").resolve("IMG_10.CR2")));

        WarehouseFiles.removeSelected(root.toString(), "IMG_1.JPG");
        assertFalse(Files.exists(selectedJpg));
        assertFalse(Files.exists(root.resolve("选片").resolve("RAW").resolve("IMG_1.CR2")));
        assertTrue(Files.exists(root.resolve("RAW").resolve("IMG_1.CR2")));
        assertTrue(Files.exists(root.resolve("IMG_1.JPG")));
    }

    @Test
    void copyRejectsSymlinkThatLeavesWarehouse() throws Exception {
        Path root = temp.resolve("bank");
        Path outside = temp.resolve("outside.jpg");
        writeJpeg(outside, 0xff0000);
        Files.createDirectories(root);
        Files.createSymbolicLink(root.resolve("escape.jpg"), outside);

        org.junit.jupiter.api.Assertions.assertThrows(java.io.IOException.class,
                () -> WarehouseFiles.copySelected(root.toString(), "escape.jpg"));
        assertFalse(Files.exists(root.resolve("选片").resolve("JPG").resolve("escape.jpg")));
    }

    @Test
    void moveIfAbsentDoesNotReplace() throws Exception {
        Path source = temp.resolve("source.jpg");
        Path target = temp.resolve("nested").resolve("source.jpg");
        writeJpeg(source, 0x010101);
        Files.createDirectories(target.getParent());
        writeJpeg(target, 0x020202);
        byte[] before = Files.readAllBytes(target);

        assertFalse(WarehouseFiles.moveIfAbsent(source, target));
        assertTrue(sameBytes(before, Files.readAllBytes(target)));
        assertTrue(Files.exists(source));
    }

    @Test
    void thumbnailWidthIsClamped() {
        assertEquals(200, WarehouseLayout.clampThumbnailWidth(10));
        assertEquals(200, WarehouseLayout.clampThumbnailWidth(200));
        assertEquals(1600, WarehouseLayout.clampThumbnailWidth(4000));
        assertEquals(800, WarehouseLayout.clampThumbnailWidth(800));
    }

    @Test
    void jpgFolderRootStripsSuffixOnly() {
        assertEquals("/data/bank", WarehouseLayout.warehouseRoot("/data/bank/JPG"));
        assertEquals("/data/bank", WarehouseLayout.warehouseRoot("/data/bank/"));
        assertEquals("/", WarehouseLayout.warehouseRoot("/JPG"));
        assertFalse(WarehouseLayout.isJpgFolder("/data/bank/JPGJPG"));
    }

    private static void writeJpeg(Path file, int color) throws Exception {
        BufferedImage image = new BufferedImage(4, 4, BufferedImage.TYPE_INT_RGB);
        for (int y = 0; y < 4; y++) {
            for (int x = 0; x < 4; x++) {
                image.setRGB(x, y, color);
            }
        }
        Files.createDirectories(file.getParent());
        ImageIO.write(image, "jpg", file.toFile());
    }

    private static boolean sameBytes(byte[] left, byte[] right) {
        return java.util.Arrays.equals(left, right);
    }
}
