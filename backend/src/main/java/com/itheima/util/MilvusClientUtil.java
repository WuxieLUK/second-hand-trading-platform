package com.itheima.util;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.CheckHealthResponse;
import io.milvus.param.ConnectParam;
import io.milvus.param.R;
import org.springframework.stereotype.Component;

/**
 * Milvus 连接工具类（适配你的本地部署环境）
 */
@Component
public class MilvusClientUtil {
    // 单例客户端
    private static MilvusServiceClient client;

    // 初始化连接（项目启动时执行）
    public static MilvusServiceClient getClient() {
        if (client == null) {
            client = new MilvusServiceClient(
                    ConnectParam.newBuilder()
                            // 你的 Milvus 地址（本地部署就是 127.0.0.1）
                            .withHost("127.0.0.1")
                            // Milvus 端口（固定 19530，和你的容器映射一致）
                            .withPort(19530)
                            .build()
            );
            // 验证连接
            R<CheckHealthResponse> health = client.checkHealth();
            if (health.getData().getIsHealthy()) {
                System.out.println("✅ Milvus 连接成功（校易通平台）");
            } else {
                throw new RuntimeException("❌ Milvus 连接失败：" + health.getMessage());
            }
        }
        return client;
    }

    // 关闭连接（项目关闭时执行）
    public static void closeClient() {
        if (client != null) {
            client.close();
            System.out.println("✅ Milvus 连接已关闭");
        }
    }
}
