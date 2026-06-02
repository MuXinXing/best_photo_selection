package com.mx;

import lombok.extern.log4j.Log4j2;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * @author MuXin
 * @create By MX-Desktop 2026/6/2 18:41
 * @system Windows 10 21H2 专业工作站版
 * @Gitee https://gitee.com/mxgitstorage
 */
@SpringBootApplication
@Log4j2
public class SelectionApplicationContextMain {
    public static void main(String[] args) {
        SpringApplication.run(SelectionApplicationContextMain.class, args);
        log.info("原神 启动");
    }
}
