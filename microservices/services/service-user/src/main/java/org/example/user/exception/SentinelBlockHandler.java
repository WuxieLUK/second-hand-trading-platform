package org.example.user.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.user.POJO.User;

public final class SentinelBlockHandler {

    private SentinelBlockHandler() {
    }

    public static Result<String> loginBlocked(String username, String password, BlockException exception) {
        return Result.error("用户服务已触发限流: " + exception.getClass().getSimpleName());
    }

    public static Result<PageBean<User>> userListBlocked(Integer pageNum, Integer pageSize, String username, String email, BlockException exception) {
        return Result.error("用户列表接口已触发限流: " + exception.getClass().getSimpleName());
    }
}
