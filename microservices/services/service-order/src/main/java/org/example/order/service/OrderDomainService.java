package org.example.order.service;

import org.example.common.PageBean;
import org.example.goods.DTO.RefundApplyDTO;
import org.example.goods.DTO.RefundHandleDTO;
import org.example.order.DTO.OrderCreateDTO;
import org.example.order.DTO.OrderQueryDTO;
import org.example.order.VO.OrderDetailVO;
import org.example.order.VO.OrderVO;

public interface OrderDomainService {
    String createOrder(OrderCreateDTO orderCreateDTO);

    PageBean<OrderVO> list(OrderQueryDTO queryDTO);

    OrderDetailVO findById(Integer id);

    void updateStatus(Integer id);

    void cancelOrder(Integer id);

    void confirmReceive(Integer id);

    void applyRefund(RefundApplyDTO refundApplyDTO);

    void handleRefund(RefundHandleDTO refundHandleDTO);

    void deleteOrder(Integer id);

    void sendOrder(Integer id);

    OrderVO findByOrderNo(String orderNo);

    void adminUpdateStatus(Integer id, Integer orderStatus);

}
