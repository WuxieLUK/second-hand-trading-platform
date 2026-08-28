package com.itheima.config;

import com.itheima.util.MilvusCollectionUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Configuration;

/**
 * Milvus 商品向量集合初始化配置
 * <p>
 * 通过配置项 {@code app.milvus.init-enabled} 控制是否在启动时初始化 Milvus：
 * <ul>
 *     <li>{@code true}（默认）：启动时连接 Milvus 并初始化商品向量集合，用于 AI 检索/知识图谱场景；</li>
 *     <li>{@code false}：跳过 Milvus 初始化，适合本地无 Milvus 环境或仅演示基础交易功能时。</li>
 * </ul>
 */
@Configuration
@ConditionalOnProperty(name = "app.milvus.init-enabled", havingValue = "true", matchIfMissing = true)
public class MilvusInitConfig {

    @Autowired
    private MilvusCollectionUtil milvusCollectionUtil;

    /**
     * 项目启动时初始化Milvus商品集合
     */
    @PostConstruct
    public void initMilvusCollection() {
        milvusCollectionUtil.initGoodsCollection();
    }
}
