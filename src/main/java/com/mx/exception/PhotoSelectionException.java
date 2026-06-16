package com.mx.exception;

/**
 * @author MuXin
 * @create By MX-Desktop 2026/3/26 10:55
 * @system Windows 10 21H2 专业工作站版
 * @Gitee https://gitee.com/mxgitstorage
 */
public class PhotoSelectionException extends Exception {

    public PhotoSelectionException() {
        super();
    }

    public PhotoSelectionException(String message) {
        super(message);
    }

    public PhotoSelectionException(String message, Throwable cause) {
        super(message, cause);
    }
}
