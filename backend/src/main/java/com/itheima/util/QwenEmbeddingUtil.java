package com.itheima.util;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * 通义千问 Embedding 工具类
 * 用于调用阿里云DashScope兼容OpenAI接口生成文本向量
 */
@Component
public class QwenEmbeddingUtil {

    // OpenAI客户端（懒加载：首次使用时创建，未配置 DASHSCOPE_API_KEY 时不影响应用启动）
    private volatile OpenAIClient openAIClient;

    /**
     * 获取 OpenAI 客户端（懒加载）
     * 未配置环境变量 DASHSCOPE_API_KEY 时抛出明确异常
     */
    private OpenAIClient getClient() {
        if (openAIClient == null) {
            String apiKey = System.getenv("DASHSCOPE_API_KEY");
            if (apiKey == null || apiKey.isEmpty()) {
                throw new IllegalStateException("未配置环境变量 DASHSCOPE_API_KEY，无法调用通义千问 Embedding 服务");
            }
            synchronized (this) {
                if (openAIClient == null) {
                    this.openAIClient = OpenAIOkHttpClient.builder()
                            .apiKey(apiKey)
                            .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                            .build();
                }
            }
        }
        return openAIClient;
    }

    /**
     * 生成单段文本的Embedding向量
     * @param text 需要生成向量的文本
     * @return 文本对应的浮点型向量列表
     * @throws Exception 调用接口时的异常
     */
    public List<Double> getEmbedding(String text) throws Exception {
        // 1. 构造 Embedding 请求参数
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .input(text)
                // 指定通义千问的 Embedding模型（dashscope 兼容模式下的模型名）
                .model("text-embedding-v3")
                .build();

        // 2. 调用同步接口获取 Embedding 响应
        CreateEmbeddingResponse response = getClient().embeddings().create(params);

        // 3. 解析响应，提取向量数据
        response.data();
        if (!response.data().isEmpty()) {
            Embedding embedding = response.data().get(0);
            List<Float> floatEmbedding = embedding.embedding();

            // 将 Float 转换为 Double
            return floatEmbedding.stream()
                    .map(Double::valueOf)
                    .toList();
        } else {
            throw new RuntimeException("获取 Embedding 向量失败：响应数据为空");
        }
    }

    /**
     * 批量生成文本的 Embedding 向量
     * @param texts 文本列表
     * @return 每个文本对应的向量列表（顺序与输入文本一致）
     * @throws Exception 调用接口时的异常
     */
    public List<List<Double>> getBatchEmbeddings(List<String> texts) throws Exception {
        if (texts == null || texts.isEmpty()) {
            throw new IllegalArgumentException("批量生成向量的文本列表不能为空");
        }

        // 过滤空文本
        List<String> validTexts = texts.stream()
                .filter(t -> t != null && !t.trim().isEmpty())
                .toList();

        if (validTexts.isEmpty()) {
            throw new IllegalArgumentException("批量生成向量的有效文本为空");
        }

        // 分批处理，每批最多 16 个文本（OpenAI API 限制）
        int batchSize = 16;
        List<List<Double>> allEmbeddings = new java.util.ArrayList<>();

        for (int i = 0; i < validTexts.size(); i += batchSize) {
            int end = Math.min(i + batchSize, validTexts.size());
            List<String> batch = validTexts.subList(i, end);

            // 为每个文本单独调用 API（避免批量接口的兼容性问题）
            for (String text : batch) {
                List<Double> embedding = getEmbedding(text);
                allEmbeddings.add(embedding);
            }
        }

        return allEmbeddings;
    }
}