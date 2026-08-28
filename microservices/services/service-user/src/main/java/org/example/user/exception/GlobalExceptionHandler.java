package org.example.user.exception;

import lombok.extern.slf4j.Slf4j;
import org.example.common.Result;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result<String>> handleException(Exception exception) {
        log.error("service-user exception", exception);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error(exception.getMessage() == null ? "服务内部错误" : exception.getMessage()));
    }
}
