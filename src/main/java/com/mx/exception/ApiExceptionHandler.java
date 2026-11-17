package com.mx.exception;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.util.LinkedHashMap;
import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(PhotoSelectionException.class)
    public ResponseEntity<Map<String, Object>> handlePhoto(PhotoSelectionException ex) {
        return ResponseEntity.status(ex.getHttpStatus()).body(body(code(ex.getHttpStatus()), ex.getMessage()));
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleBadRequest(IllegalArgumentException ex) {
        return ResponseEntity.badRequest().body(body("BAD_REQUEST", ex.getMessage()));
    }

    private static Map<String, Object> body(String code, String message) {
        Map<String, Object> body = new LinkedHashMap<>();
        body.put("code", code);
        body.put("message", message == null ? "" : message);
        return body;
    }

    private static String code(int status) {
        if (status == 404) {
            return "NOT_FOUND";
        }
        if (status == 409) {
            return "CONFLICT";
        }
        if (status == 400) {
            return "BAD_REQUEST";
        }
        return "ERROR";
    }
}
