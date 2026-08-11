package com.glass.agent.web;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 把工具参数校验抛出的 {@link IllegalArgumentException} 统一转换为 400，
 * 与原 TypeScript 版本的错误信息保持一致（形如「参数 xxx 必须是...」）。
 */
@RestControllerAdvice
public class ApiExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Map<String, Object>> handleIllegalArgument(IllegalArgumentException ex) {
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Map.of("error", "错误：" + ex.getMessage(), "isError", true));
    }
}
