package org.example.common.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * 全局异常处理模板（参考实现）
 * 注意：common 模块作为纯工具模块，不声明可扫描的 Spring Bean
 * 各服务使用自己模块内的 GlobalExceptionHandler
 */
@Slf4j
//@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(IllegalArgumentException.class)
    public ResponseEntity<Result<String>> handleIllegalArgument(IllegalArgumentException exception) {
        log.warn("参数校验异常", exception);
        return ResponseEntity.status(HttpStatus.BAD_REQUEST)
                .body(Result.error(exception.getMessage()));
    }

    @ExceptionHandler(IllegalStateException.class)
    public ResponseEntity<Result<String>> handleIllegalState(IllegalStateException exception) {
        log.warn("业务状态异常", exception);
        return ResponseEntity.status(HttpStatus.CONFLICT)
                .body(Result.error(exception.getMessage()));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception exception) {
        log.error("服务内部错误", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(exception.getMessage() == null ? "服务内部错误" : exception.getMessage()));
    }
}
