package org.example.product.feign;

import org.example.common.Result;
import org.example.order.VO.OrderDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

/**
 * service-order 的 Feign 客户端
 * 供 Agent 的 OrderQueryTool 调用订单数据
 */
@FeignClient(name = "service-order")
public interface OrderFeignClient {

    /**
     * 根据订单ID查询订单详情
     */
    @GetMapping("/order/detail/{id}")
    Result<OrderDetailVO> getOrderDetail(@PathVariable("id") Integer id);
}
