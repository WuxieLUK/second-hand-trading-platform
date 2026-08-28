package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.DTO.OrderCreateDTO;
import com.itheima.DTO.OrderQueryDTO;
import com.itheima.DTO.RefundApplyDTO;
import com.itheima.DTO.RefundHandleDTO;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.OrderService;
import com.itheima.vo.OrderDetailVO;
import com.itheima.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

/**
 * 订单控制器（无关联表，含退款接口）
 */
@RestController
@RequestMapping("/order")
@Slf4j
@Tag(name = "订单接口", description = "二手交易订单管理接口（含退款）")
public class OrderController {
    @Resource
    private OrderService orderService;

    // 1. 创建订单
    @PreAuthorize("/order/create")
    @PostMapping("/create")
    @Operation(summary = "创建订单", description = "二手商品下单（一对一）")
    public Result<String> createOrder(@RequestBody @Validated OrderCreateDTO orderCreateDTO) {
        String orderNo = orderService.createOrder(orderCreateDTO);
        return Result.success(orderNo);
    }

    // 2. 查询订单列表
    @PreAuthorize("/order/list")
    @GetMapping("/list")
    @Operation(summary = "查询订单列表", description = "分页查询订单（支持订单状态、退款状态筛选）")
    public Result<PageBean<OrderVO>> list(OrderQueryDTO queryDTO) {
        PageBean<OrderVO> pb = orderService.list(queryDTO);
        return Result.success(pb);
    }

    // 3. 订单详情
    @PreAuthorize("/order/detail")
    @GetMapping("/detail/{id}")
    @Operation(summary = "订单详情", description = "查询订单详细信息（含退款信息）")
    public Result<OrderDetailVO> detail(@PathVariable Integer id) {
        OrderDetailVO vo = orderService.findById(id);
        return Result.success(vo);
    }

    // 4. 更新订单状态（支付/发货/完成）
    @PreAuthorize("/order/updateStatus")
    @PutMapping("/status/{id}")
    @Operation(summary = "更新订单状态", description = "支付/发货/完成等状态更新")
    public Result updateStatus(@PathVariable Integer id) {
        orderService.updateStatus(id);
        return Result.success("订单状态更新成功");
    }

    // 5. 取消订单
    @PreAuthorize("/order/cancel")
    @PutMapping("/cancel/{id}")
    @Operation(summary = "取消订单", description = "取消待付款/待发货订单（无退款中）")
    public Result cancelOrder(@PathVariable Integer id) {
        orderService.cancelOrder(id);
        return Result.success("订单取消成功");
    }

    // 6. 确认收货
    @PreAuthorize("/order/confirmReceive")
    @PutMapping("/confirm/{id}")
    @Operation(summary = "确认收货", description = "确认收到商品（无退款中）")
    public Result confirmReceive(@PathVariable Integer id) {
        orderService.confirmReceive(id);
        return Result.success("确认收货成功");
    }

    // 7. 申请退款（核心新增）
    @PreAuthorize("/order/refund/apply")
    @PostMapping("/refund/apply")
    @Operation(summary = "申请退款", description = "买家申请退款（需已支付、无退款记录）")
    public Result applyRefund(@RequestBody @Validated RefundApplyDTO refundApplyDTO) {
        orderService.applyRefund(refundApplyDTO);
        return Result.success("退款申请提交成功，请等待处理");
    }

    // 8. 处理退款（核心新增）
    @PreAuthorize("/order/refund/handle")
    @PutMapping("/refund/handle")
    @Operation(summary = "处理退款", description = "商家/平台处理退款申请（同意/驳回）")
    public Result handleRefund(@RequestBody @Validated RefundHandleDTO refundHandleDTO) {
        orderService.handleRefund(refundHandleDTO);
        return Result.success("退款处理完成");
    }

    //删除订单
    @PreAuthorize("/order/delete")
    @DeleteMapping("/delete/{id}")
    @Operation(summary = "删除订单", description = "删除订单")
    public Result deleteOrder(@PathVariable Integer id) {
        orderService.deleteOrder(id);
        return Result.success("删除订单成功");
    }


    //订单发货
    @PreAuthorize("/order/send/{id}")
    @PutMapping("/send/{id}")
    @Operation(summary = "订单发货", description = "订单发货")
    public Result sendOrder(@PathVariable Integer id) {
        orderService.sendOrder(id);
        return Result.success("订单发货成功");
    }

    //根据订单编号查询某一条订单
    @PreAuthorize("/order/findByOrderNo/{orderNo}")
    @GetMapping("/findByOrderNo/{orderNo}")
    @Operation(summary = "根据订单编号查询某条订单", description = "根据订单编号查询某条订单")
    public Result<OrderVO> findByOrderNo(@PathVariable String orderNo) {
        OrderVO orderVO = orderService.findByOrderNo(orderNo);
        return Result.success(orderVO);
    }

    //管理员修改商品状态
    @PreAuthorize("/order/adminUpdateStatus/{id}")
    @PutMapping("/adminUpdateStatus/{id}/{status}")
    @Operation(summary = "修改商品状态", description = "修改商品状态")
    public Result adminUpdateStatus(@PathVariable Integer id, @PathVariable Integer status) {
        orderService.adminUpdateStatus(id, status);
        return Result.success("修改商品状态成功");
    }
}