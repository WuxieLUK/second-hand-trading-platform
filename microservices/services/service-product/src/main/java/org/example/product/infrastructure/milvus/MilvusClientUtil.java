package org.example.product.infrastructure.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.CheckHealthResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import org.springframework.stereotype.Component;

/**
 * Milvus 连接工具类（从 service-main 迁移）
 */
@Component
public class MilvusClientUtil {

    private static MilvusServiceClient client;

    public static MilvusServiceClient getClient() {
        if (client == null) {
            client = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            .withHost("127.0.0.1")
                            .withPort(19530)
                            .build()
            );
            R<CheckHealthResponse> health = client.checkHealth();
            if (!health.getData().getIsHealthy()) {
                throw new RuntimeException("Milvus 连接异常");
            }
        }
        return client;
    }
}
