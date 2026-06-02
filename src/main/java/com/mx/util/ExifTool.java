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
 * @author MuXin
 * @create By MX-Desktop 2026/3/25 15:56
 * @system Windows 10 21H2 专业工作站版
 * @Gitee https://gitee.com/mxgitstorage
 */
@Component
public class ExifTool {

    /**
     * @Author MX
     * @function 获取单张图片exif信息
     * */
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

    /**
     * @Author MX
     * @function 获取指定的EXIF信息
     * */
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
