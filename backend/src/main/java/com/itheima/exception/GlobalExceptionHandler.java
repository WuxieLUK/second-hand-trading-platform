package com.itheima.exception;

import com.itheima.pojo.Result;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.context.request.async.AsyncRequestNotUsableException;
import org.apache.catalina.connector.ClientAbortException;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@RestControllerAdvice
public class GlobalExceptionHandler {

    /**
     * 处理客户端连接中断异常（无需返回结果，记录日志即可）
     */
    @ExceptionHandler({ClientAbortException.class, AsyncRequestNotUsableException.class})
    public ResponseEntity<Void> handleClientAbortException(Exception e, HttpServletRequest request) {
        log.warn("客户端连接中断：URI={}, IP={}, 异常：{}",
                request.getRequestURI(),
                request.getRemoteAddr(),
                e.getMessage());
        // 无需返回响应，客户端已断开
        return ResponseEntity.status(HttpStatus.NO_CONTENT).build();
    }

    /**
     * 其他全局异常
     */
    @ExceptionHandler(Exception.class)
    public ResponseEntity<Result> handleException(Exception e) {
        log.error("系统异常", e);
        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(Result.error("服务器内部错误"));
    }
}
