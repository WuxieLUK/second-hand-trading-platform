package org.example.product.infrastructure.ai;

import com.openai.client.OpenAIClient;
import com.openai.client.okhttp.OpenAIOkHttpClient;
import com.openai.models.embeddings.CreateEmbeddingResponse;
import com.openai.models.embeddings.Embedding;
import com.openai.models.embeddings.EmbeddingCreateParams;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Embedding 工具类（从 service-main 迁移）
 */
@Component
public class QwenEmbeddingUtil {

    private final OpenAIClient openAIClient;

    public QwenEmbeddingUtil() {
        this.openAIClient = OpenAIOkHttpClient.builder()
                .apiKey(System.getenv("DASHSCOPE_API_KEY"))
                .baseUrl("https://dashscope.aliyuncs.com/compatible-mode/v1")
                .build();
    }

    public List<Double> getEmbedding(String text) {
        EmbeddingCreateParams params = EmbeddingCreateParams.builder()
                .input(text)
                .model("text-embedding-v3")
                .build();
        CreateEmbeddingResponse response = openAIClient.embeddings().create(params);
        if (!response.data().isEmpty()) {
            Embedding embedding = response.data().get(0);
            return embedding.embedding().stream().map(Double::valueOf).toList();
        }
        throw new RuntimeException("获取 Embedding 向量失败");
    }
}
