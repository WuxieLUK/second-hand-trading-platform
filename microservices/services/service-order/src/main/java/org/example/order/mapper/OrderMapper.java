package org.example.order.mapper;

import org.apache.ibatis.annotations.Delete;
import org.apache.ibatis.annotations.Insert;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Options;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;
import org.example.order.DTO.OrderQueryDTO;
import org.example.order.POJO.OrderInfo;
import org.example.order.VO.OrderVO;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface OrderMapper {

    @Insert("INSERT INTO order_info (order_no, buyer_id, seller_id, address_id, goods_id, goods_name, goods_pic, goods_price, goods_num, total_amount, order_status, pay_type, remark, refund_status, refund_amount, create_time, update_time) VALUES (#{orderNo}, #{buyerId}, #{sellerId}, #{addressId}, #{goodsId}, #{goodsName}, #{goodsPic}, #{goodsPrice}, #{goodsNum}, #{totalAmount}, #{orderStatus}, #{payType}, #{remark}, 0, 0.00, #{createTime}, #{updateTime})")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void add(OrderInfo orderInfo);

    List<OrderVO> list(OrderQueryDTO queryDTO);

    OrderInfo findById(Integer id);

    @Update("UPDATE order_info SET order_status = #{status}, pay_type = #{payType}, pay_time = #{payTime}, update_time = #{payTime} WHERE id = #{id}")
    void updateStatus(@Param("id") Integer id, @Param("status") Integer status, @Param("payType") Integer payType, @Param("payTime") LocalDateTime payTime);

    @Update("UPDATE order_info SET refund_status = #{refundStatus}, refund_amount = #{refundAmount}, refund_reason = #{refundReason}, refund_time = #{refundTime}, refund_remark = #{refundRemark}, update_time = #{updateTime} WHERE id = #{orderId}")
    void applyRefund(@Param("orderId") Integer orderId,
                     @Param("refundStatus") Integer refundStatus,
                     @Param("refundAmount") BigDecimal refundAmount,
                     @Param("refundReason") String refundReason,
                     @Param("refundTime") LocalDateTime refundTime,
                     @Param("refundRemark") String refundRemark,
                     @Param("updateTime") LocalDateTime updateTime);

    @Update("UPDATE order_info SET refund_status = #{handleResult}, refund_remark = #{refundRemark}, refund_time = #{refundTime}, update_time = #{updateTime} WHERE id = #{orderId}")
    void handleRefund(@Param("orderId") Integer orderId,
                      @Param("handleResult") Integer handleResult,
                      @Param("refundRemark") String refundRemark,
                      @Param("refundTime") LocalDateTime refundTime,
                      @Param("updateTime") LocalDateTime updateTime);

    @Delete("DELETE FROM order_info WHERE id = #{id}")
    void deleteOrder(Integer id);

    @Update("UPDATE order_info SET cancel_time = now(), order_status = #{orderStatus}, update_time = now() WHERE id = #{id}")
    void updateCancelStatus(@Param("id") Integer id, @Param("orderStatus") Integer orderStatus);

    @Update("UPDATE order_info SET receive_time = now(), order_status = #{orderStatus}, update_time = now() WHERE id = #{id}")
    void updateConfirmStatus(@Param("id") Integer id, @Param("orderStatus") Integer orderStatus);

    @Update("UPDATE order_info SET delivery_time = #{now}, order_status = #{orderStatus}, update_time = #{now} WHERE id = #{id}")
    void updateSendStatus(@Param("id") Integer id, @Param("orderStatus") Integer orderStatus, @Param("now") LocalDateTime now);

    @Select("SELECT * FROM order_info WHERE order_no = #{orderNo}")
    OrderVO findByOrderNo(String orderNo);

    OrderInfo findLatestByGoodsId(Integer goodsId);

    @Update("UPDATE order_info SET order_status = #{orderStatus}, update_time = now() WHERE id = #{id}")
    void adminUpdateStatus(@Param("id") Integer id, @Param("orderStatus") Integer orderStatus);
}
