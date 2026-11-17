package com.mx.util;

import com.drew.imaging.ImageMetadataReader;
import com.drew.metadata.Metadata;
import com.drew.metadata.exif.ExifIFD0Directory;
import com.drew.metadata.exif.ExifSubIFDDirectory;
import com.mx.model.bo.PhotoExif;
import lombok.extern.log4j.Log4j2;
import org.springframework.stereotype.Component;

import java.io.File;

/**
 * 读取 JPG 的 EXIF。机身在 IFD0，曝光参数在 SubIFD。读失败时返回空结果，不中断扫描。
 */
@Component
@Log4j2
public class ExifTool {

    public String readDateTaken(String fileName) {
        return readerSpecificExif(fileName).getDateTaken();
    }

    public PhotoExif readerSpecificExif(String fileName) {
        PhotoExif photoExif = new PhotoExif();
        try {
            Metadata metadata = ImageMetadataReader.readMetadata(new File(fileName));
            ExifIFD0Directory ifd0 = metadata.getFirstDirectoryOfType(ExifIFD0Directory.class);
            ExifSubIFDDirectory sub = metadata.getFirstDirectoryOfType(ExifSubIFDDirectory.class);
            if (ifd0 != null) {
                photoExif.setCameraModel(ifd0.getDescription(ExifIFD0Directory.TAG_MODEL));
                photoExif.setLenInfo(ifd0.getDescription(ExifIFD0Directory.TAG_LENS_MODEL));
                if (photoExif.getDateTaken() == null) {
                    photoExif.setDateTaken(ifd0.getDescription(ExifIFD0Directory.TAG_DATETIME));
                }
            }
            if (sub != null) {
                photoExif.setFNumber(sub.getDescription(ExifSubIFDDirectory.TAG_FNUMBER));
                photoExif.setShutterSpeed(sub.getDescription(ExifSubIFDDirectory.TAG_EXPOSURE_TIME));
                if (photoExif.getShutterSpeed() == null) {
                    photoExif.setShutterSpeed(sub.getDescription(ExifSubIFDDirectory.TAG_SHUTTER_SPEED));
                }
                photoExif.setIsoSpeed(sub.getDescription(ExifSubIFDDirectory.TAG_ISO_EQUIVALENT));
                photoExif.setFlash(sub.getDescription(ExifSubIFDDirectory.TAG_FLASH));
                photoExif.setFocalLength(sub.getDescription(ExifSubIFDDirectory.TAG_FOCAL_LENGTH));
                if (photoExif.getLenInfo() == null) {
                    photoExif.setLenInfo(sub.getDescription(ExifSubIFDDirectory.TAG_LENS_MODEL));
                }
                String original = sub.getDescription(ExifSubIFDDirectory.TAG_DATETIME_ORIGINAL);
                if (original != null) {
                    photoExif.setDateTaken(original);
                }
            }
        } catch (Exception e) {
            log.debug("读取 EXIF 失败 {}: {}", fileName, e.getMessage());
        }
        return photoExif;
    }
}
