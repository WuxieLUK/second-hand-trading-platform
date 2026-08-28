package org.example.product.infrastructure.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.example.goods.VO.GoodsVO;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通义千问聊天工具 —— 生成 RAG 搜索结果的自然语言总结
 * <p>
 * 从 service-main 迁移而来，使用 DashScope 的 qwen-turbo 模型。
 * 模型统一为 qwen-turbo（轻量版，速度快，适合短文本总结）。
 */
@Component
public class QwenChatUtil {

    private final OpenAIClient openAIClient;

    public QwenChatUtil() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
    }

    /**
     * 生成RAG搜索结果的自然语言总结
     */
    public String generateSearchSummary(String query, List<GoodsVO> goodsList) {
        if (goodsList.isEmpty()) {
            return String.format("未找到与「%s」相关的在售二手商品", query);
        }

        StringBuilder goodsInfo = new StringBuilder();
        for (int i = 0; i < Math.min(5, goodsList.size()); i++) {
            GoodsVO goods = goodsList.get(i);
            goodsInfo.append(String.format(
                    "商品%d：名称=%s，价格=%.2f元，新旧程度=%s，库存=%d件\n",
                    i+1,
                    goods.getGoodsName(),
                    goods.getSellPrice(),
                    goods.getIsNewName(),
                    goods.getStock()
            ));
        }

        String prompt = String.format(
                "你是二手交易平台的智能搜索助手，请根据用户查询和检索到的商品信息，生成友好的搜索结果总结。\n" +
                        "用户查询：%s\n" +
                        "商品信息：\n%s\n" +
                        "要求：1. 语气友好；2. 突出核心信息（价格、新旧、库存）；3. 控制在100字以内；4. 只返回总结内容，不要额外说明。",
                query, goodsInfo.toString()
        );

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model("qwen-turbo")
                .temperature(0.3)
                .maxTokens(200)
                .build();

        ChatCompletion response = openAIClient.chat().completions().create(params);
        return response.choices().get(0).message().content().orElse("抱歉，未能生成有效的回复");
    }
}
