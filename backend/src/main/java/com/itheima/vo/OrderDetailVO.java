package com.itheima.vo;

import lombok.Data;

import java.io.Serializable;
import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * 订单详情VO（含完整退款信息）
 */
@Data
public class OrderDetailVO implements Serializable {
    private Integer id;
    private String orderNo;
    // 买家信息
    private Integer buyerId;
    private String buyerNickname;
    private String buyerPhone;
    // 卖家信息
    private Integer sellerId;
    private String sellerNickname;
    private String sellerPhone;
    // 收货地址
    private String address;
    // 商品信息
    private Integer goodsId;
    private String goodsName;
    private String goodsPic;
    private BigDecimal goodsPrice;
    private Integer goodsNum;
    // 订单信息
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private String orderStatusName;
    private Integer payType;
    private String payTypeName;
    private LocalDateTime payTime;
    private LocalDateTime deliveryTime;
    private LocalDateTime receiveTime;
    private LocalDateTime cancelTime;
    private String remark;
    // 退款信息（核心新增）
    private Integer refundStatus;
    private String refundStatusName;
    private BigDecimal refundAmount;
    private String refundReason;
    private LocalDateTime refundTime;
    private String refundRemark;
    // 公共字段
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}