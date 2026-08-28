package org.example.product.config;

import jakarta.annotation.PostConstruct;
import org.example.product.infrastructure.milvus.MilvusCollectionUtil;
import org.springframework.context.annotation.Configuration;

@Configuration
public class MilvusInitConfig {

    private final MilvusCollectionUtil milvusCollectionUtil;

    public MilvusInitConfig(MilvusCollectionUtil milvusCollectionUtil) {
        this.milvusCollectionUtil = milvusCollectionUtil;
    }

    @PostConstruct
    public void initMilvusCollection() {
        milvusCollectionUtil.initGoodsCollection();
    }
}
