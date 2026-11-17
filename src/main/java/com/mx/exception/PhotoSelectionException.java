package com.mx.exception;

/** 选片业务异常。httpStatus 由统一异常处理转成响应。 */
public class PhotoSelectionException extends RuntimeException {

    private final int httpStatus;

    public PhotoSelectionException(String message) {
        this(message, 404);
    }

    public PhotoSelectionException(String message, int httpStatus) {
        super(message);
        this.httpStatus = httpStatus;
    }

    public PhotoSelectionException(String message, Throwable cause) {
        this(message, 500, cause);
    }

    public PhotoSelectionException(String message, int httpStatus, Throwable cause) {
        super(message, cause);
        this.httpStatus = httpStatus;
    }

    public int getHttpStatus() {
        return httpStatus;
    }
}
