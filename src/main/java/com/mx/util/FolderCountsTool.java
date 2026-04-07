package com.mx.util;

import lombok.Data;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

/**
 * @author MuXin
 * @create By MX-Desktop 2026/3/26 10:25
 * @system Windows 10 21H2 专业工作站版E
 * @Gitee https://gitee.com/mxgitstorage
 */

@Data
public class FolderCountsTool {

    public int folderCounts(String path) {
        int fileCounts;
        String[] list = new File(path).list();

        if (list != null) {
            fileCounts = list.length;
        } else {
            return 0;
        }
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
}
