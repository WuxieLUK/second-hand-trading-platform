package org.example.collect.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.VO.GoodsVO;

public final class SentinelBlockHandler {

    private SentinelBlockHandler() {
    }

    public static Result<Void> collectAddBlocked(Integer goodsId, BlockException exception) {
        return Result.error("收藏服务已触发限流: " + exception.getClass().getSimpleName());
    }

    public static Result<PageBean<GoodsVO>> collectListBlocked(Integer pageNum, Integer pageSize, BlockException exception) {
        return Result.error("收藏列表已触发限流: " + exception.getClass().getSimpleName());
    }
}
