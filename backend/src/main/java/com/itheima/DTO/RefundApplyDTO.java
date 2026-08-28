package com.itheima.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;

/**
 * 退款申请DTO（核心新增）
 */
@Data
public class RefundApplyDTO {
    @NotNull(message = "订单ID不能为空")
    private Integer orderId; // 订单ID

    @NotNull(message = "退款金额不能为空")
    private BigDecimal refundAmount; // 退款金额（二手默认全额）

    @NotNull(message = "退款原因不能为空")
    private String refundReason; // 退款原因（如：商品不符、质量问题等）

    private String refundRemark; // 退款备注（可选）
}