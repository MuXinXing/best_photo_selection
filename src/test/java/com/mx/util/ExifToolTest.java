package com.mx.util;

import com.mx.model.bo.PhotoExif;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.nio.file.Files;
import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNull;

class ExifToolTest {

    @TempDir
    Path temp;

    @Test
    void missingFileAndPlainJpegDoNotThrow() throws Exception {
        ExifTool exifTool = new ExifTool();
        assertNull(exifTool.readDateTaken(temp.resolve("missing.jpg").toString()));

        Path jpeg = temp.resolve("plain.jpg");
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        Files.createDirectories(jpeg.getParent());
        ImageIO.write(image, "jpg", jpeg.toFile());

        PhotoExif exif = assertDoesNotThrow(() -> exifTool.readerSpecificExif(jpeg.toString()));
        assertNull(exif.getCameraModel());
        assertNull(exif.getFNumber());
    }
}
