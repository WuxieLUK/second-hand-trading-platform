package com.itheima.DTO;

import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

/**
 * 订单查询DTO（含退款状态筛选）
 */
@Data
public class OrderQueryDTO {
    private Integer pageNum = 1; // 页码
    private Integer pageSize = 10; // 每页条数

    private String orderNo; // 订单编号（模糊查询）
    private Integer buyerId; // 买家ID
    private Integer sellerId; // 卖家ID
    private Integer goodsId; // 商品ID（按商品查订单）
    private Integer orderStatus; // 订单状态
    private Integer refundStatus; // 新增：退款状态筛选
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime startTime; // 创建时间-开始
    @DateTimeFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'")
    private LocalDateTime endTime; // 创建时间-结束
}
