package org.example.product.service.impl;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import lombok.extern.slf4j.Slf4j;
import org.example.goods.POJO.Goods;
import org.example.product.mapper.GoodsMapper;
import org.example.product.service.AiService;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

/**
 * AI 对话服务实现（从 service-main 迁移）
 * 使用本地 Ollama 部署的模型
 */
@Slf4j
@Service
public class AiServiceImpl implements AiService {

    private final GoodsMapper goodsMapper;

    public AiServiceImpl(GoodsMapper goodsMapper) {
        this.goodsMapper = goodsMapper;
    }

    private OpenAIClient getOpenAIClient() {
        return OpenAIOkHttpClient.builder()
                .apiKey("ollama-local")
                .baseUrl("http://localhost:11434/v1")
                .build();
    }

    private String getRealTimeBlogContext() {
        try {
            List<Goods> onSaleGoods = goodsMapper.listOnSaleGoods();
            if (onSaleGoods.isEmpty()) {
                return "暂无在售商品";
            }
            StringBuilder goodsInfo = new StringBuilder("【在售商品列表】：\n");
            for (int i = 0; i < Math.min(onSaleGoods.size(), 10); i++) {
                Goods goods = onSaleGoods.get(i);
                String newDegree = switch (goods.getIsNew()) {
                    case 0 -> "二手"; case 1 -> "全新"; case 2 -> "9成新";
                    case 3 -> "8成新"; case 4 -> "7成及以下"; default -> "二手";
                };
                goodsInfo.append(String.format(
                        "%d. **%s**，新旧程度：%s，售价¥%.2f，库存%d件\n",
                        i + 1, goods.getGoodsName(), newDegree,
                        goods.getSellPrice(), goods.getStock()
                ));
            }
            goodsInfo.append("\n【平台规则】：\n");
            goodsInfo.append("- 交易支持7天无理由退换（需保持商品原状）；\n");
            goodsInfo.append("- 二手商品价格可议价，联系卖家需通过平台聊天窗口。");
            return goodsInfo.toString();
        } catch (Exception e) {
            return "实时商品数据加载失败，暂无可用信息";
        }
    }

    @Override
    public Flux<String> getAiResponse(String question, String blogContext) {
        OpenAIClient client = getOpenAIClient();
        String realTimeContext = (blogContext != null && !blogContext.isBlank())
                ? blogContext : getRealTimeBlogContext();
        String systemRole = String.format("""
                你是「校园二手交易平台」的AI客服，熟悉平台所有规则和商品信息。
                【实时数据】：%s
                【回答规则】：
                1. 优先使用提供的实时数据回答，数据中没有的信息不编造；
                2. 如果数据不足，引导用户询问商品、价格、库存等问题；
                3. 回答简洁专业，使用Markdown格式。
                """, realTimeContext);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage(systemRole)
                .addUserMessage(question)
                .model("qwen3:4b")
                .temperature(0.4)
                .maxTokens(3000)
                .build();

        return Flux.create(sink -> {
            StreamResponse<ChatCompletionChunk> response = null;
            try {
                response = client.chat().completions().createStreaming(params);
                response.stream().forEach(chunk -> {
                    if (chunk.choices() != null && !chunk.choices().isEmpty()) {
                        chunk.choices().get(0).delta().content().ifPresent(content -> {
                            if (content != null && !content.isBlank()) {
                                sink.next(content);
                            }
                        });
                    }
                });
                sink.complete();
            } catch (Exception e) {
                sink.next("AI 客服响应异常，请稍后重试。");
                sink.error(new RuntimeException("AI客服响应异常：" + e.getMessage()));
            } finally {
                if (response != null) {
                    try { response.close(); } catch (Exception ignored) {}
                }
            }
        });
    }

    @Override
    public String generateGoodsDesc(String keywords, String goodsName, Integer isNew, BigDecimal sellPrice) {
        try {
            return generateByOllama(keywords, goodsName, isNew, sellPrice);
        } catch (Exception e) {
            log.warn("Ollama 不可用，使用模板兜底生成描述：{}", e.getMessage());
            return generateFallbackDesc(keywords, goodsName, isNew, sellPrice);
        }
    }

    /**
     * 通过 Ollama 生成商品描述
     */
    private String generateByOllama(String keywords, String goodsName, Integer isNew, BigDecimal sellPrice) {
        OpenAIClient client = getOpenAIClient();
        String newDegree = formatNewDegree(isNew);

        String prompt = String.format(
                "你是二手交易平台的商品描述生成助手，请根据以下信息生成专业、详细的商品描述：\n" +
                "商品名称：%s\n新旧程度：%s\n售价：%s元\n关键词：%s\n" +
                "要求：语气友好，200-500字，包含商品状态和卖点，仅返回描述文本。",
                goodsName, newDegree, sellPrice, keywords);

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model("qwen3:4b")
                .temperature(0.6)
                .maxTokens(1000)
                .build();

        ChatCompletion response = client.chat().completions().create(params);
        return response.choices().get(0).message().content().orElse("");
    }

    /**
     * 模板兜底：当 AI 服务不可用时生成基础描述
     */
    private String generateFallbackDesc(String keywords, String goodsName, Integer isNew, BigDecimal sellPrice) {
        String newDegree = formatNewDegree(isNew);
        StringBuilder desc = new StringBuilder();

        desc.append("<h3>").append(goodsName).append("</h3>");
        desc.append("<p>").append("【商品概况】").append(goodsName);

        // 新旧程度
        desc.append("，").append(newDegree);

        // 价格
        desc.append("，现售价 <strong>¥").append(sellPrice).append("</strong>。").append("</p>");

        // 核心卖点（从关键词中提取）
        if (keywords != null && !keywords.isBlank()) {
            desc.append("<p>【核心卖点】<br/>");
            String[] kwList = keywords.split("[，,、]");
            for (int i = 0; i < Math.min(kwList.length, 5); i++) {
                String kw = kwList[i].trim();
                if (!kw.isBlank()) {
                    desc.append("• ").append(kw).append("<br/>");
                }
            }
            desc.append("</p>");
        }

        // 交易说明
        desc.append("<p>【交易说明】<br/>");
        desc.append("• 商品为个人自用闲置转卖，实物拍摄，介意慎拍<br/>");
        desc.append("• 支持校园内当面验货，确认无误后交易<br/>");
        desc.append("• 已购买即表示接受商品现状，非质量问题不退不换<br/>");
        desc.append("• 如有任何疑问，欢迎通过平台聊天窗口联系卖家</p>");

        // 结尾
        desc.append("<p>更多细节请查看商品图片或联系卖家咨询，谢谢！</p>");

        return desc.toString();
    }

    private String formatNewDegree(Integer isNew) {
        if (isNew == null) return "二手";
        return switch (isNew) {
            case 0 -> "二手";
            case 1 -> "全新";
            case 2 -> "9成新";
            case 3 -> "8成新";
            case 4 -> "7成及以下";
            default -> "二手";
        };
    }
}
