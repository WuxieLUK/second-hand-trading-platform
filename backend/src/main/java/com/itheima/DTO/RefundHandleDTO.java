package com.itheima.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 退款处理DTO（商家/平台处理退款）
 */
@Data
public class RefundHandleDTO {
    @NotNull(message = "订单ID不能为空")
    private Integer orderId; // 订单ID

    @NotNull(message = "处理结果不能为空")
    private Integer handleResult; // 处理结果：2-退款成功 3-退款失败

    private String refundRemark; // 处理备注（如：同意退款、驳回原因等）
}