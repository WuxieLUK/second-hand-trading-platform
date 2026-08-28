package org.example.product.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

/**
 * 平台规则工具 —— 供 LangChain4j Agent 调用
 * <p>
 * 从 service-main 迁移而来，提供静态平台规则文本。
 */
@Slf4j
@Component
public class PlatformRuleTool {

    private static final String PLATFORM_RULES = """
            📋 **校园二手交易平台 - 交易规则与政策**

            **一、交易流程**
            1. 买家浏览商品 → 联系卖家 → 下单支付 → 等待卖家发货 → 确认收货 → 完成交易
            2. 卖家发布商品 → 等待买家下单 → 发货 → 等待买家确认 → 收到款项

            **二、支付说明**
            - 平台支持微信支付和支付宝支付两种方式
            - 订单提交后请在24小时内完成支付，超时自动取消
            - 支付完成后订单状态变为「待发货」

            **三、发货与收货**
            - 卖家在收到订单后应在48小时内发货
            - 买家收到商品后请及时确认收货
            - 若超过7天未确认收货，系统将自动确认收货

            **四、退款规则**
            - 买家可在卖家发货前申请全额退款
            - 卖家发货后，买家需与卖家协商退款事宜
            - 退款申请提交后，卖家需在48小时内处理
            - 若卖家超时未处理，系统将自动退款给买家
            - 退款金额默认等同商品金额

            **五、商品管理**
            - 商品状态：1-在售 2-已售出 3-下架 4-审核中 5-违规封禁
            - 在售商品超过30天未成交将自动下架
            - 禁止销售违禁品，一经发现立即封禁账号

            **六、用户行为规范**
            - 请保持友善沟通，禁止恶意骚扰、欺诈行为
            - 交易纠纷可联系平台客服协助处理
            - 多次违规将限制账号功能或封禁账号
            """;

    /**
     * Agent 工具：获取平台交易规则
     */
    @Tool("Get the campus second-hand trading platform rules and policies, including transaction流程, payment, shipping, refund, and user guidelines.")
    public String getPlatformRules() {
        log.info("Agent 调用 PlatformRuleTool");
        return PLATFORM_RULES;
    }
}
