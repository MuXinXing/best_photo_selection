package com.mx.util;

import lombok.Data;
import org.springframework.stereotype.Component;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author MuXin
 * @create By MX-Desktop 2026/3/26 10:25
 * @system Windows 10 21H2 专业工作站版
 * @Gitee https://gitee.com/mxgitstorage
 */

@Data
@Component
public class FolderTool {

    private int files;
    private String[] fileList;
    private String folderPath;

    public int folderCounts(String path) {
        int fileCounts;
        String[] list = new File(path).list();
        fileList = list;
        folderPath = path;
        if (list != null) {
            fileCounts = list.length;
        } else {
            return 0;
        }
        files = list.length;
        return fileCounts;
    }

    public long folderSizeCount(String folderPath) {
        AtomicLong folderSize = new AtomicLong();
        try {
            Path path = Paths.get(folderPath);
            Files.walk(path)
                    .filter(Files::isRegularFile)
                    .forEach(dir ->{
                        try {
                            BasicFileAttributes attributes = Files.readAttributes(dir,BasicFileAttributes.class);
                            folderSize.addAndGet( attributes.size());
                        } catch (IOException e) {
                            throw new RuntimeException(e);
                        }
                    });
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
        return folderSize.get() / (1024*1024);
    }

    public void copyFilesToFolder(String path) {

    }
}
