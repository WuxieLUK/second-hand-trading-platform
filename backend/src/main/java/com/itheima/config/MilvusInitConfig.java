package com.itheima.config;

import com.itheima.util.MilvusCollectionUtil;
import jakarta.annotation.PostConstruct;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Configuration;

@Configuration
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