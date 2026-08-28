package com.itheima.util;

import com.itheima.vo.GoodsVO;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QwenChatUtil {

    private final OpenAIClient openAIClient;

    public QwenChatUtil() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY")) // 从环境变量获取API Key
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
    }

    /**
     * 生成RAG搜索结果的自然语言总结
     * @param query 用户查询文本
     * @param goodsList 检索到的商品列表
     * @return 人性化的搜索总结
     */
    public String generateSearchSummary(String query, List<GoodsVO> goodsList) {
        if (goodsList.isEmpty()) {
            return String.format("未找到与「%s」相关的在售二手商品", query);
        }

        // 拼接商品信息
        StringBuilder goodsInfo = new StringBuilder();
        for (int i = 0; i < Math.min(5, goodsList.size()); i++) {
            com.itheima.vo.GoodsVO goods = goodsList.get(i);
            goodsInfo.append(String.format(
                    "商品%d：名称=%s，价格=%.2f元，新旧程度=%s，库存=%d件\n",
                    i+1,
                    goods.getGoodsName(),
                    goods.getSellPrice(),
                    goods.getIsNewName(),
                    goods.getStock()
            ));
        }

        // 构造千问提示词
        String prompt = String.format(
                "你是二手交易平台的智能搜索助手，请根据用户查询和检索到的商品信息，生成友好的搜索结果总结。\n" +
                        "用户查询：%s\n" +
                        "商品信息：\n%s\n" +
                        "要求：1. 语气友好；2. 突出核心信息（价格、新旧、库存）；3. 控制在100字以内；4. 只返回总结内容，不要额外说明。",
                query, goodsInfo.toString()
        );

        // 调用千问API
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model("qwen-turbo") // 轻量版模型，速度快
                .temperature(0.3) // 低随机性，保证结果稳定
                .maxTokens(200)
                .build();

        ChatCompletion response = openAIClient.chat().completions().create(params);
        return response.choices().get(0).message().content().orElse("抱歉，未能生成有效的回复");

    }

    /**
     * 新增：根据用户行为生成结构化用户标签
     * @param userId 用户ID（用于日志）
     * @param userBehavior  用户行为文本（浏览/购买/发布的商品信息拼接）
     * @return 结构化标签：如“专业：计算机，年级：大三，兴趣：编程、电子产品、机械键盘”
     */
    public String generateUserTags(Long userId, String userBehavior) {
        // 空行为直接返回默认标签
        if (userBehavior == null || userBehavior.trim().isEmpty()) {
            return "未识别到有效行为标签";
        }

        // 构造千问提示词（强制结构化输出，便于后续解析）
        String prompt = String.format(
                "你是校园二手交易平台的用户标签生成助手，请根据用户的商品行为记录，生成结构化的用户标签。\n" +
                        "用户行为记录：%s\n" +
                        "生成规则：\n" +
                        "1. 必须包含【专业】（如计算机、汉语言、机械）、【年级】（如大一、大二、大三、大四）、【兴趣】（如编程、考研、美妆）三个维度；\n" +
                        "2. 无法识别的维度填「未知」，兴趣维度至少1个，最多5个；\n" +
                        "3. 输出格式严格遵循：专业：XXX，年级：XXX，兴趣：XXX、XXX、XXX；\n" +
                        "4. 标签简洁，每个兴趣不超过4个字，总长度不超过100字；\n" +
                        "5. 只返回标签内容，不要额外说明。",
                userBehavior
        );

        try {
            ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                    .addUserMessage(prompt)
                    .model("qwen-turbo") // 轻量版模型，速度快
                    .temperature(0.1) // 极低随机性，保证输出格式稳定
                    .maxTokens(100)
                    .build();

            ChatCompletion response = openAIClient.chat().completions().create(params);
            String tags = response.choices().get(0).message().content().orElse("专业：未知，年级：未知，兴趣：未知");
            // 校验输出格式（防止模型乱输出）
            if (!tags.contains("专业：") || !tags.contains("年级：") || !tags.contains("兴趣：")) {
                tags = "专业：未知，年级：未知，兴趣：未知";
            }
            return tags;
        } catch (Exception e) {
            // 异常时返回默认标签
            return "专业：未知，年级：未知，兴趣：未知";
        }
    }


}