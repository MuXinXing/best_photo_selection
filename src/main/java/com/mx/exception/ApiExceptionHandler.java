package com.mx.exception;

import lombok.extern.log4j.Log4j2;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.MethodArgumentTypeMismatchException;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
@Log4j2
public class ApiExceptionHandler {

    @ExceptionHandler(PhotoSelectionException.class)
    public ResponseEntity<Map<String, Object>> handlePhoto(PhotoSelectionException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(body(code(ex.getHttpStatus()), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        String message = ex.getMessage();
        if (message == null || message.isBlank()) {
            message = "请求参数不正确";
        }
        return ResponseEntity.badRequest().body(body("请求无效", message));
    }

    @ExceptionHandler(HttpMessageNotReadableException.class)
    public ResponseEntity<Map<String, Object>> handleUnreadable(HttpMessageNotReadableException ex) {
        log.debug("请求体无法解析: {}", ex.getMessage());
        return ResponseEntity.badRequest().body(body("请求无效", "请求体格式不正确"));
    }

    @ExceptionHandler(MethodArgumentTypeMismatchException.class)
    public ResponseEntity<Map<String, Object>> handleTypeMismatch(MethodArgumentTypeMismatchException ex) {
        return ResponseEntity.badRequest().body(body("请求无效", "参数格式不正确"));
    }

    @ExceptionHandler(MissingServletRequestParameterException.class)
    public ResponseEntity<Map<String, Object>> handleMissingParam(MissingServletRequestParameterException ex) {
        return ResponseEntity.badRequest().body(body("请求无效", "缺少必要参数"));
    }

    @ExceptionHandler(HttpRequestMethodNotSupportedException.class)
    public ResponseEntity<Map<String, Object>> handleMethod(HttpRequestMethodNotSupportedException ex) {
        return ResponseEntity.status(405).body(body("请求无效", "不支持该请求方法"));
    }

    @ExceptionHandler(NoResourceFoundException.class)
    public ResponseEntity<Map<String, Object>> handleNotFound(NoResourceFoundException ex) {
        return ResponseEntity.status(404).body(body("未找到", "接口不存在"));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Map<String, Object>> handleUnexpected(Exception ex) {
        log.error("未处理的异常", ex);
        return ResponseEntity.internalServerError().body(body("服务器错误", "服务器内部错误"));
    }

    private static Map<String, Object> body(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message == null ? "" : message);
        return body;
    }

    private static String code(int status) {
        if (status == 404) {
            return "未找到";
        }
        if (status == 409) {
            return "冲突";
        }
        if (status == 400) {
            return "请求无效";
        }
        return "服务器错误";
    }
}
