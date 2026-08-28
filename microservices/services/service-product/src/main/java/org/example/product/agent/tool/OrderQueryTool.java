package org.example.product.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.example.common.Result;
import org.example.order.VO.OrderDetailVO;
import org.example.product.feign.OrderFeignClient;
import org.springframework.stereotype.Component;

/**
 * 订单查询工具 —— 供 LangChain4j Agent 调用
 * <p>
 * 从 service-main 迁移而来，通过 Feign 调用 service-order 获取订单数据。
 */
@Slf4j
@Component
public class OrderQueryTool {

    private final OrderFeignClient orderFeignClient;

    public OrderQueryTool(OrderFeignClient orderFeignClient) {
        this.orderFeignClient = orderFeignClient;
    }

    /**
     * Agent 工具：根据订单ID查询订单详情
     */
    @Tool("Query order details by order ID. Returns order status, goods info, payment info, and refund info.")
    public String getOrderDetail(Integer orderId) {
        log.info("Agent 调用 OrderQueryTool: orderId={}", orderId);

        if (orderId == null || orderId <= 0) {
            return "订单ID无效，请提供正确的订单编号。";
        }

        try {
            Result<OrderDetailVO> result = orderFeignClient.getOrderDetail(orderId);
            if (result.getCode() != 0 || result.getData() == null) {
                return String.format("订单ID为 %d 的订单不存在。", orderId);
            }

            OrderDetailVO order = result.getData();
            String orderStatusText = formatOrderStatus(order.getOrderStatus());
            String payTypeText = formatPayType(order.getPayType());
            String refundStatusText = formatRefundStatus(order.getRefundStatus());

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("**订单 %s** 的详细信息：\n\n", order.getOrderNo()));
            sb.append(String.format("- **订单ID**：%d\n", order.getId()));
            sb.append(String.format("- **订单编号**：%s\n", order.getOrderNo()));
            sb.append(String.format("- **订单状态**：%s\n", orderStatusText));
            sb.append(String.format("- **商品**：%s（数量：%d）\n", order.getGoodsName(), order.getGoodsNum()));
            sb.append(String.format("- **商品价格**：¥%.2f\n", order.getGoodsPrice()));
            sb.append(String.format("- **总金额**：¥%.2f\n", order.getTotalAmount()));
            sb.append(String.format("- **支付方式**：%s\n", payTypeText));

            if (order.getPayTime() != null) {
                sb.append(String.format("- **支付时间**：%s\n", order.getPayTime()));
            }
            if (order.getDeliveryTime() != null) {
                sb.append(String.format("- **发货时间**：%s\n", order.getDeliveryTime()));
            }
            if (order.getReceiveTime() != null) {
                sb.append(String.format("- **收货时间**：%s\n", order.getReceiveTime()));
            }
            if (order.getCancelTime() != null) {
                sb.append(String.format("- **取消时间**：%s\n", order.getCancelTime()));
            }
            if (order.getRemark() != null && !order.getRemark().isBlank()) {
                sb.append(String.format("- **备注**：%s\n", order.getRemark()));
            }

            // 退款信息
            if (order.getRefundStatus() != null && order.getRefundStatus() > 0) {
                sb.append("\n**退款信息：**\n");
                sb.append(String.format("- **退款状态**：%s\n", refundStatusText));
                if (order.getRefundAmount() != null) {
                    sb.append(String.format("- **退款金额**：¥%.2f\n", order.getRefundAmount()));
                }
                if (order.getRefundReason() != null) {
                    sb.append(String.format("- **退款原因**：%s\n", order.getRefundReason()));
                }
            }

            sb.append(String.format("\n- **创建时间**：%s\n", order.getCreateTime()));
            return sb.toString();

        } catch (Exception e) {
            log.error("查询订单详情失败, orderId={}", orderId, e);
            return "查询订单详情时遇到系统异常，请稍后重试。";
        }
    }

    private String formatOrderStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "待付款";
            case 2 -> "待发货";
            case 3 -> "待收货";
            case 4 -> "已完成";
            case 5 -> "已取消";
            default -> "未知";
        };
    }

    private String formatPayType(Integer payType) {
        if (payType == null || payType == 0) return "未支付";
        return switch (payType) {
            case 1 -> "微信支付";
            case 2 -> "支付宝支付";
            default -> "未知";
        };
    }

    private String formatRefundStatus(Integer status) {
        if (status == null) return "无退款";
        return switch (status) {
            case 0 -> "无退款";
            case 1 -> "退款中";
            case 2 -> "退款成功";
            case 3 -> "退款失败";
            default -> "未知";
        };
    }
}
