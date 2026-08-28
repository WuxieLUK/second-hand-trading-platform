package org.example.product.service;

import reactor.core.publisher.Flux;

import java.math.BigDecimal;

/**
 * AI 对话服务接口（从 service-main 迁移）
 */
public interface AiService {
    Flux<String> getAiResponse(String message, String blogContext);
    String generateGoodsDesc(String keywords, String goodsName, Integer isNew, BigDecimal sellPrice);
}
