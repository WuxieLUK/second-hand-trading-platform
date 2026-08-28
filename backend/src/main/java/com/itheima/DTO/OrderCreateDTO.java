package com.itheima.DTO;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 订单创建DTO（二手交易：单商品）
 */
@Data
public class OrderCreateDTO {
    @NotNull(message = "买家ID不能为空")
    private Integer buyerId; // 买家ID

    @NotNull(message = "卖家ID不能为空")
    private Integer sellerId; // 卖家ID

    @NotNull(message = "收货地址ID不能为空")
    private Integer addressId; // 收货地址ID

    @NotNull(message = "商品ID不能为空")
    private Integer goodsId; // 商品ID（核心字段）

    private Integer goodsNum = 1; // 商品数量（二手默认1）

    private String remark; // 订单备注

    // 新增requestId字段，用于幂等性校验
    private String requestId;
}