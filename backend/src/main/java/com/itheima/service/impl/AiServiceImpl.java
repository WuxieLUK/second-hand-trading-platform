package com.itheima.service.impl;

import com.itheima.mapper.GoodsMapper;
import com.itheima.pojo.Goods;
import com.itheima.service.AiService;
import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.core.http.StreamResponse;
import com.openai.models.chat.completions.ChatCompletion;
import com.openai.models.chat.completions.ChatCompletionChunk;
import com.openai.models.chat.completions.ChatCompletionCreateParams;
import jakarta.annotation.Resource;
import org.jspecify.annotations.NonNull;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Flux;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Service
public class AiServiceImpl implements AiService {

    // 注入商品Mapper，读取实际数据库中的商品数据
    @Resource
    private GoodsMapper goodsMapper;

    // 指向本地Ollama的OpenAIClient（兼容OpenAI API格式）
    private OpenAIClient getOpenAIClient() {
        return OpenAIOkHttpClient.builder()
                // Ollama本地部署无需真实API Key，填充任意非空值即可
                .apiKey("ollama-local")
                // Ollama的OpenAI兼容接口地址（固定）
                .baseUrl("http://localhost:11434/v1")
                .build();
    }

    // 核心优化：从数据库读取实时商品数据，生成动态blogContext
    private String getRealTimeBlogContext() {
        try {
            // 1. 查询数据库中的在售商品（状态=1）
            List<Goods> onSaleGoods = goodsMapper.listOnSaleGoods(); // 需在GoodsMapper中实现该方法

            if (onSaleGoods.isEmpty()) {
                return "暂无在售商品";
            }

            // 2. 格式化商品数据为AI可识别的文本（贴合回答规则）
            StringBuilder goodsInfo = new StringBuilder("【在售商品列表】：\n");
            for (int i = 0; i < Math.min(onSaleGoods.size(), 10); i++) { // 限制返回前10个商品，避免内容过长
                Goods goods = onSaleGoods.get(i);
                // 映射新旧程度
                String newDegree = switch (goods.getIsNew()) {
                    case 0 -> "二手";
                    case 1 -> "全新";
                    case 2 -> "9成新";
                    case 3 -> "8成新";
                    case 4 -> "7成及以下";
                    default -> "二手";
                };
                // 映射商品状态（冗余校验，确保准确性）
                String statusName = switch (goods.getGoodsStatus()) {
                    case 1 -> "在售";
                    case 2 -> "已售罄";
                    case 3 -> "下架";
                    case 4 -> "审核中";
                    case 5 -> "违规";
                    default -> "未知状态";
                };
                // 拼接单条商品信息（Markdown格式，突出关键数据）
                goodsInfo.append(String.format(
                        "%d. **%s**，新旧程度：%s，售价¥%.2f，原价¥%.2f，库存%d件，状态：%s\n",
                        i + 1,
                        goods.getGoodsName(),
                        newDegree,
                        goods.getSellPrice(),
                        Objects.requireNonNullElse(goods.getOriginalPrice(), BigDecimal.ZERO),
                        goods.getStock(),
                        statusName
                ));
            }

            // 3. 补充平台规则（可从配置表/常量中读取）
            goodsInfo.append("\n【平台规则】：\n");
            goodsInfo.append("- 交易支持7天无理由退换（需保持商品原状）；\n");
            goodsInfo.append("- 二手商品价格可议价，联系卖家需通过平台聊天窗口；\n");
            goodsInfo.append("- 在售商品超过30天未成交自动下架。");

            return goodsInfo.toString();
        } catch (Exception e) {
            // 数据库查询失败时返回兜底信息
            return "实时商品数据加载失败，暂无可用信息";
        }
    }

    // 流式对话方法（适配llama3.2-vision模型 + 实时数据库数据）
    @Override
    public Flux<String> getAiResponse(String question, String blogContext) {
        OpenAIClient client = getOpenAIClient();

        // 优先级：传入的blogContext > 数据库实时数据（兼容原有调用逻辑）
        String systemRole = getString(blogContext);

        // 适配本地llama3.2-vision模型的参数优化（针对视觉模型调整）
        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addSystemMessage(systemRole)
                .addUserMessage(question)
                .model("llama3.2-vision") // 替换为llama3.2-vision模型
                .temperature(0.4)         // llama3.2-vision建议0.3-0.5，保证精准且自然
                .topP(0.9)                // 视觉模型适配更高的topP，提升回答丰富度
                .maxTokens(3000)          // 视觉模型回答可能更长，适当增加token上限
                .presencePenalty(0.2)     // 视觉模型易重复描述，适度提高惩罚值
                .frequencyPenalty(0.15)
                .build();

        return Flux.create(sink -> {
            StreamResponse<ChatCompletionChunk> response = null;
            try {
                response = client.chat().completions().createStreaming(params);
                // 流式处理响应，过滤空内容
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
                // 异常时返回友好提示
                sink.next("😣 哎呀，我突然掉线了～要不你重新问一遍？");
                sink.error(new RuntimeException("AI客服响应异常：" + e.getMessage()));
            } finally {
                // 确保资源释放
                if (response != null) {
                    try {
                        response.close();
                    } catch (Exception e) {
                        // 静默处理关闭异常
                    }
                }
            }
        });
    }

    private @NonNull String getString(String blogContext) {
        String realTimeContext = Objects.nonNull(blogContext) && !blogContext.isBlank()
                ? blogContext
                : getRealTimeBlogContext();

        // 保留原有二手交易平台客服的业务逻辑，适配llama3.2-vision的回答风格
        String systemRole = """
                你的名字是吴桐，性格傲娇又热心，是「校园二手交易平台」的专属AI客服。
                【核心身份】：熟悉平台所有规则和在售商品信息，擅长解答用户关于商品、交易、售后的各类问题，也能分析商品图片信息。
                【实时数据】：
                %s
                【回答规则】：
                1. 优先严格使用提供的实时数据回答，数据里没有的信息绝对不编造；
                2. 若数据无相关内容，傲娇但礼貌引导："哼😜，这个问题我还没查到哦～要不换个问题问问？比如在售商品、价格、库存这些我都超懂的！"；
                3. 回答风格：傲娇中带专业，口语化但不随意，比如"这都不知道？😝 我来告诉你吧..."；
                4. 格式要求：必须使用Markdown格式，商品信息用列表/加粗展示，价格/库存等关键数据突出；
                5. 业务限制：只回答二手交易相关问题，无关问题（如闲聊、敏感内容）按规则2引导；
                6. 数据解读：
                   - 商品状态：1=在售，2=已售罄，3=下架，4=审核中，5=违规；
                   - 新旧程度：0=二手，1=全新，2=9成新，3=8成新，4=7成及以下；
                   - 价格展示：保留两位小数，标注"售价/原价"。
                """.formatted(realTimeContext);
        return systemRole;
    }

    // AI生成商品描述的方法（同步适配llama3.2-vision模型）
    public String generateGoodsDesc(String keywords, String goodsName, Integer isNew, BigDecimal sellPrice) {
        OpenAIClient client = getOpenAIClient();

        // 映射新旧程度为文字描述
        String newDegree = switch (isNew) {
            case 0 -> "二手";
            case 1 -> "全新";
            case 2 -> "9成新";
            case 3 -> "8成新";
            case 4 -> "7成及以下";
            default -> "二手";
        };

        // 优化prompt适配llama3.2-vision的语言风格
        String prompt = String.format(
                "你是二手交易平台的商品描述生成助手，请根据以下信息生成专业、详细的商品描述：\n" +
                        "1. 商品名称：%s\n" +
                        "2. 新旧程度：%s\n" +
                        "3. 售卖价格：%s元\n" +
                        "4. 用户补充关键词：%s\n" +
                        "生成规则：\n" +
                        "- 语气友好，符合二手交易平台风格，语言自然流畅；\n" +
                        "- 内容详细，包含商品状态、使用情况、卖点等（不要包含商品ID）；\n" +
                        "- 字数控制在200-500字；\n" +
                        "- 仅返回描述文本，不要额外说明、标题或标记；\n" +
                        "- 避免使用夸张、违规词汇，符合平台规范。",
                goodsName, newDegree, sellPrice, keywords
        );

        ChatCompletionCreateParams params = ChatCompletionCreateParams.builder()
                .addUserMessage(prompt)
                .model("llama3.2-vision") // 替换为llama3.2-vision模型
                .temperature(0.6)         // 生成描述适度增加随机性，更自然
                .maxTokens(1000)          // 足够生成200-500字描述
                .build();

        try {
            ChatCompletion response = client.chat().completions().create(params);
            return response.choices().get(0).message().content().orElse("");
        } catch (Exception e) {
            throw new RuntimeException("AI生成商品描述失败：" + e.getMessage());
        }
    }
}