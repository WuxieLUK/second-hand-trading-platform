package org.example.comment.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.POJO.Comment;

public final class SentinelBlockHandler {

    private SentinelBlockHandler() {
    }

    public static Result<Void> commentAddBlocked(Comment comment, BlockException exception) {
        return Result.error("评论服务已触发限流: " + exception.getClass().getSimpleName());
    }

    public static Result<PageBean<Comment>> commentListBlocked(Integer pageNum, Integer pageSize, Integer goodsId, String content, String username, BlockException exception) {
        return Result.error("评论列表已触发限流: " + exception.getClass().getSimpleName());
    }
}
