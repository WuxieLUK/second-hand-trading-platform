package org.example.order.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import jakarta.validation.Valid;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.RefundApplyDTO;
import org.example.goods.DTO.RefundHandleDTO;
import org.example.order.DTO.OrderCreateDTO;
import org.example.order.DTO.OrderQueryDTO;
import org.example.order.VO.OrderDetailVO;
import org.example.order.VO.OrderVO;
import org.example.order.exception.SentinelBlockHandler;
import org.example.order.service.OrderDomainService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/order")
public class OrderController {

    private final OrderDomainService orderDomainService;

    public OrderController(OrderDomainService orderDomainService) {
        this.orderDomainService = orderDomainService;
    }

    @PostMapping("/create")
    @SentinelResource(value = "orderCreate", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "createOrderBlocked")
    public Result<String> createOrder(@RequestBody @Valid OrderCreateDTO orderCreateDTO) {
        return Result.success(orderDomainService.createOrder(orderCreateDTO));
    }

    @GetMapping("/list")
    @SentinelResource(value = "orderList", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "orderListBlocked")
    public Result<PageBean<OrderVO>> list(OrderQueryDTO queryDTO) {
        return Result.success(orderDomainService.list(queryDTO));
    }

    @GetMapping("/detail/{id}")
    public Result<OrderDetailVO> detail(@PathVariable Integer id) {
        return Result.success(orderDomainService.findById(id));
    }

    @PutMapping("/status/{id}")
    public Result<Void> updateStatus(@PathVariable Integer id) {
        orderDomainService.updateStatus(id);
        return Result.success();
    }

    @PutMapping("/cancel/{id}")
    public Result<Void> cancelOrder(@PathVariable Integer id) {
        orderDomainService.cancelOrder(id);
        return Result.success();
    }

    @PutMapping("/confirm/{id}")
    public Result<Void> confirmReceive(@PathVariable Integer id) {
        orderDomainService.confirmReceive(id);
        return Result.success();
    }

    @PostMapping("/refund/apply")
    public Result<Void> applyRefund(@RequestBody @Valid RefundApplyDTO refundApplyDTO) {
        orderDomainService.applyRefund(refundApplyDTO);
        return Result.success();
    }

    @PutMapping("/refund/handle")
    public Result<Void> handleRefund(@RequestBody @Valid RefundHandleDTO refundHandleDTO) {
        orderDomainService.handleRefund(refundHandleDTO);
        return Result.success();
    }

    @DeleteMapping("/delete/{id}")
    public Result<Void> deleteOrder(@PathVariable Integer id) {
        orderDomainService.deleteOrder(id);
        return Result.success();
    }

    @PutMapping("/send/{id}")
    public Result<Void> sendOrder(@PathVariable Integer id) {
        orderDomainService.sendOrder(id);
        return Result.success();
    }

    @GetMapping("/findByOrderNo/{orderNo}")
    public Result<OrderVO> findByOrderNo(@PathVariable String orderNo) {
        return Result.success(orderDomainService.findByOrderNo(orderNo));
    }

    @PutMapping("/adminUpdateStatus/{id}/{status}")
    public Result<Void> adminUpdateStatus(@PathVariable Integer id, @PathVariable Integer status) {
        orderDomainService.adminUpdateStatus(id, status);
        return Result.success();
    }

}
