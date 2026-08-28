package com.itheima.mapper;

import com.itheima.DTO.OrderQueryDTO;
import com.itheima.pojo.OrderInfo;
import com.itheima.vo.OrderVO;
import org.apache.ibatis.annotations.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

/**
 * 订单Mapper（无关联表，含退款逻辑）
 */
public interface OrderMapper {
    // 1. 创建订单（含商品冗余字段）
    @Insert("INSERT INTO order_info (order_no, buyer_id, seller_id, address_id, goods_id, goods_name, goods_pic, goods_price, goods_num, total_amount, order_status, pay_type, remark, refund_status, refund_amount, create_time, update_time) " +
            "VALUES (#{orderNo}, #{buyerId}, #{sellerId}, #{addressId}, #{goodsId}, #{goodsName}, #{goodsPic}, #{goodsPrice}, #{goodsNum}, #{totalAmount}, #{orderStatus}, #{payType}, #{remark}, 0, 0.00, #{createTime}, #{updateTime})")
    void add(OrderInfo orderInfo);

    // 2. 分页查询订单（含退款状态筛选）
    List<OrderVO> list(OrderQueryDTO queryDTO);

    // 3. 根据ID查询订单（含退款字段）
    OrderInfo findById(Integer id);

    // 4. 修改订单（非状态类字段，含退款字段）
    @Update("UPDATE order_info SET " +
            "remark = #{remark}, " +
            "pay_time = #{payTime}, " +
            "delivery_time = #{deliveryTime}, " +
            "receive_time = #{receiveTime}, " +
            "cancel_time = #{cancelTime}, " +
            "refund_status = #{refundStatus}, " +
            "refund_amount = #{refundAmount}, " +
            "refund_reason = #{refundReason}, " +
            "refund_time = #{refundTime}, " +
            "refund_remark = #{refundRemark}, " +
            "update_time = #{updateTime} " +
            "WHERE id = #{id}")
    void update(OrderInfo orderInfo);

    // 5. 支付订单
    @Update("UPDATE order_info SET " +
            "order_status = #{status}, " +
            "pay_type = #{payType}," +
            "pay_time = #{payTime} " +
            "WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("payType") Integer payType,@Param("payTime") LocalDateTime payTime);

    // 6. 申请退款（更新退款状态为“退款中”）
    @Update("UPDATE order_info SET " +
            "refund_status = #{refundStatus}, " +
            "refund_amount = #{refundAmount}, " +
            "refund_reason = #{refundReason}, " +
            "refund_time = #{refundTime}, " +
            "refund_remark = #{refundRemark}, " +
            "update_time = #{updateTime} " +
            "WHERE id = #{orderId}")
    void applyRefund(
            @Param("orderId") Integer orderId,
            @Param("refundStatus") Integer refundStatus,
            @Param("refundAmount") BigDecimal refundAmount,
            @Param("refundReason") String refundReason,
            @Param("refundTime") LocalDateTime refundTime,
            @Param("refundRemark") String refundRemark,
            @Param("updateTime") LocalDateTime updateTime
    );

    // 7. 处理退款（更新退款状态、退款时间、备注）
    @Update("UPDATE order_info SET " +
            "refund_status = #{handleResult}, " +
            "refund_remark = CONCAT(refund_remark, ' | 处理备注：', #{refundRemark}), " +
            "refund_time = #{refundTime}, " +
            "update_time = #{updateTime} " +
            "WHERE id = #{orderId}")
    void handleRefund(
            @Param("orderId") Integer orderId,
            @Param("handleResult") Integer handleResult,
            @Param("refundRemark") String refundRemark,
            @Param("refundTime") LocalDateTime refundTime,
            @Param("updateTime") LocalDateTime updateTime
    );


    // 8. 删除订单（仅删除订单信息，不删除商品信息）
    @Delete("DELETE FROM order_info WHERE id = #{id}")
    void deleteOrder(Integer id);


    // 9. 修改订单取消状态
    @Update("UPDATE order_info SET cancel_time = now(), order_status = #{orderStatus} WHERE id = #{id}")
    void updateCancelStatus(Integer id, Integer orderStatus);

    // 10. 修改订单确认状态
    @Update("UPDATE order_info SET receive_time = now(), order_status = #{orderStatus} WHERE id = #{id}")
    void updateConfirmStatus(Integer id, Integer orderStatus);

    // 11. 修改订单发货状态
    @Update("UPDATE order_info SET delivery_time = #{now}, order_status = #{orderStatus} WHERE id = #{id}")
    void updateSendStatus(Integer id, Integer orderStatus, LocalDateTime now);

    // 12. 根据订单编号查询订单
    @Select("SELECT * FROM order_info WHERE order_no = #{orderNo}")
    OrderVO findByOrderNo(String orderNo);


    // 13. 管理员修改订单状态
    @Update("UPDATE order_info SET order_status = #{orderStatus},update_time=now() WHERE id = #{id}")
    void adminUpdateStatus(Integer id, Integer orderStatus);
}