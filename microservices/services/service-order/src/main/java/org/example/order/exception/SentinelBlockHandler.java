package org.example.order.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.order.DTO.OrderCreateDTO;
import org.example.order.DTO.OrderQueryDTO;
import org.example.order.VO.OrderVO;

public final class SentinelBlockHandler {

    private SentinelBlockHandler() {
    }

    public static Result<String> createOrderBlocked(OrderCreateDTO orderCreateDTO, BlockException exception) {
        return Result.error("订单创建已触发限流: " + exception.getClass().getSimpleName());
    }

    public static Result<PageBean<OrderVO>> orderListBlocked(OrderQueryDTO queryDTO, BlockException exception) {
        return Result.error("订单列表已触发限流: " + exception.getClass().getSimpleName());
    }
}
