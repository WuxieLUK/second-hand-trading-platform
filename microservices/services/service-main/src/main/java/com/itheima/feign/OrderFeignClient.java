package com.itheima.feign;

import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.RefundApplyDTO;
import org.example.goods.DTO.RefundHandleDTO;
import org.example.order.DTO.OrderCreateDTO;
import org.example.order.DTO.OrderQueryDTO;
import org.example.order.VO.OrderDetailVO;
import org.example.order.VO.OrderVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

/**
 * service-order Feign 客户端（BFF 层调用）
 */
@FeignClient(name = "service-order")
public interface OrderFeignClient {

    @PostMapping("/order/create")
    Result<String> create(@RequestBody OrderCreateDTO orderCreateDTO);

    @PostMapping("/order/list")
    Result<PageBean<OrderVO>> list(@RequestBody OrderQueryDTO queryDTO);

    @GetMapping("/order/detail/{id}")
    Result<OrderDetailVO> detail(@PathVariable("id") Integer id);

    @PostMapping("/order/cancel/{id}")
    Result<Void> cancel(@PathVariable("id") Integer id);

    @PostMapping("/order/confirm/{id}")
    Result<Void> confirm(@PathVariable("id") Integer id);

    @PostMapping("/order/refund/apply")
    Result<Void> refundApply(@RequestBody RefundApplyDTO refundApplyDTO);

    @PostMapping("/order/refund/handle")
    Result<Void> refundHandle(@RequestBody RefundHandleDTO refundHandleDTO);

    @PostMapping("/order/delete/{id}")
    Result<Void> delete(@PathVariable("id") Integer id);

    @PostMapping("/order/send/{id}")
    Result<Void> send(@PathVariable("id") Integer id);
}
