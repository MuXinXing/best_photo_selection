package com.mx.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.imaging.ImageProcessingException;
import com.drew.metadata.Directory;
import com.drew.metadata.Metadata;
import com.drew.metadata.Tag;
import com.drew.metadata.exif.ExifDescriptorBase;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.mx.entity.PhotoExif;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

/**
 * 照片 EXIF 信息读取工具（基于 metadata-extractor 库）
 */
@Component
public class ExifTool {

    /** 打印所有 EXIF 标签（调试用） */
    public void readerExif(String fileName) {
        File photoFile = new File(fileName);
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(photoFile);
            for (Directory directory : metadata.getDirectories()) {
                for (Tag tag : directory.getTags()) {
                    System.out.println(tag);
                }
            }
        } catch (ImageProcessingException | IOException e) {
            throw new RuntimeException(e);
        }
    }

    /** 读取指定的 EXIF 信息：相机型号、光圈、快门、ISO、闪光灯、焦距、镜头、拍摄时间 */
    public PhotoExif readerSpecificExif(String fileName) {
        File file = new File(fileName);
        PhotoExif photoExif = new PhotoExif();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(file);
            ExifSubIFDDirectory directory = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (directory == null) {
                return photoExif;
            }
            String makerNote = directory.getDescription(ExifSubIFDDirectory.TAG_MODEL);
            String fNumber = directory.getDescription(ExifSubIFDDirectory.TAG_APERTURE);
            String shutterSpeed = directory.getDescription(ExifSubIFDDirectory.TAG_SHUTTER_SPEED);
            String isoSpeed = directory.getDescription(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT);
            String flash = directory.getDescription(ExifSubIFDDirectory.TAG_FLASH);
            String focalLength = directory.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH);
            String lenInfo = directory.getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL);
            String dateTaken = directory.getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
            photoExif.setMakerNote(makerNote);
            photoExif.setFNumber(fNumber);
            photoExif.setShutterSpeed(shutterSpeed);
            photoExif.setIsoSpeed(isoSpeed);
            photoExif.setFlash(flash);
            photoExif.setFocalLength(focalLength);
            photoExif.setLenInfo(lenInfo);
            photoExif.setDateTaken(dateTaken);
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
        return photoExif;
    }
}
