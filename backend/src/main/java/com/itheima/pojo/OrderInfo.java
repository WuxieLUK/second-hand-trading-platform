package com.itheima.pojo;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单主表实体（二手交易：一对一关联商品，含退款逻辑）
 */
@Data
public class OrderInfo {
    private Integer id; // 订单ID
    private String orderNo; // 订单编号
    private Integer buyerId; // 买家ID
    private Integer sellerId; // 卖家ID
    private Integer addressId; // 收货地址ID
    // ===== 商品相关字段（冗余存储，无关联表）=====
    private Integer goodsId; // 关联商品ID
    private String goodsName; // 商品名称
    private String goodsPic; // 商品图片
    private BigDecimal goodsPrice; // 商品价格
    private Integer goodsNum; // 商品数量（二手默认1）
    // ===== 订单核心字段 =====
    private BigDecimal totalAmount; // 订单总金额（=goodsPrice*goodsNum）
    private Integer orderStatus; // 订单状态：1-待付款 2-待发货 3-待收货 4-已完成 5-已取消
    private Integer payType; // 支付方式：0-未支付 1-微信 2-支付宝
    private LocalDateTime payTime; // 支付时间
    private LocalDateTime deliveryTime; // 发货时间
    private LocalDateTime receiveTime; // 收货时间
    private LocalDateTime cancelTime; // 取消时间
    private String remark; // 订单备注
    // ===== 退款相关字段（核心新增）=====
    private Integer refundStatus; // 退款状态：0-无退款 1-退款中 2-退款成功 3-退款失败
    private BigDecimal refundAmount; // 退款金额（默认等于总金额，二手多为全额退款）
    private String refundReason; // 退款原因
    private LocalDateTime refundTime; // 退款时间
    private String refundRemark; // 退款备注
    // ===== 公共字段 =====
    private LocalDateTime createTime; // 创建时间
    private LocalDateTime updateTime; // 修改时间
}