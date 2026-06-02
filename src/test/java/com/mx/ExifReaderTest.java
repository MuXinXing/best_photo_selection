package com.mx;

import com.mx.util.ExifTool;
import com.mx.util.FolderTool;
import org.junit.Test;

import java.util.Arrays;

/**
 * @author MuXin
 * @create By MX-Desktop 2026/3/25 16:29
 * @system Windows 10 21H2 专业工作站版
 * @Gitee https://gitee.com/mxgitstorage
 */


public class ExifReaderTest {

    private final FolderTool folderTool = new FolderTool();
    private final ExifTool exifTool = new ExifTool();
    private final String filePath = "/home/mx/下载/超分辨率壁纸/";

    @Test
    public void ReaderTest() {
        ExifTool exifTool = new ExifTool();
        String fileName = "/home/mx/IDEA_Projects/best_photo_selection/src/main/resources/static/jpg/wallhaven-3l6y9y.jpg";
        exifTool.readerExif(fileName);
    }

    @Test
    public void folderTest() {
        int size = folderTool.folderCounts(filePath);
        System.out.println(folderTool.getFolderPath());
        String[] list = folderTool.getFileList();
        System.out.println(Arrays.toString(list));
        System.out.println(size);
    }

    @Test
    public void folderSizeTest() {
        long i = folderTool.folderSizeCount(filePath);
        System.out.println("路径："+filePath+"\n文件夹大小为："+i +" MB");
    }

    @Test
    public void photoExifTest(){
        String fileName = "/home/mx/下载/2026_02_12/IMG_4808.JPG";
        Object object = exifTool.readerSpecificExif(fileName);
        System.out.println(object);
    }
}
