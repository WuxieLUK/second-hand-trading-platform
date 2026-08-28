package org.example.common.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.common.Result;

/**
 * Sentinel 限流降级统一处理器
 * 各服务可继承此类或在 @SentinelResource 中引用
 */
public final class SentinelBlockHandler {

    private SentinelBlockHandler() {
    }

    public static Result<Void> defaultBlocked(BlockException exception) {
        return Result.error("请求已被限流: " + exception.getClass().getSimpleName());
    }
}
