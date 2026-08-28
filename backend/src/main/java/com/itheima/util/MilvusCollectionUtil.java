package com.itheima.util;

import io.milvus.client.MilvusServiceClient;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.grpc.DataType;
import io.milvus.param.R;
import io.milvus.param.index.CreateIndexParam;
import org.springframework.stereotype.Component;

/**
 * Milvus 商品向量集合工具类
 * 负责创建/检查商品向量集合
 */
@Component
public class MilvusCollectionUtil {

    // 商品向量集合名称
    public static final String GOODS_COLLECTION_NAME = "second_hand_goods_embedding";
    // 知识图谱向量集合名称（复用命名规范）
    public static final String KG_COLLECTION_NAME = "campus_knowledge_graph_embedding";
    // 向量维度（text-embedding-v3 模型固定为 1024 维）
    public static final int VECTOR_DIMENSION = 1024;

    /**
     * 检查并创建商品向量集合
     */
    public void initGoodsCollection() {
        try {
            MilvusServiceClient client = MilvusClientUtil.getClient();

            // 1. 检查集合是否已存在
            R<DescribeCollectionResponse> descResponse = client.describeCollection(
                    DescribeCollectionParam.newBuilder()
                            .withCollectionName(GOODS_COLLECTION_NAME)
                            .build()
            );

            if (descResponse.getStatus() == R.Status.Success.getCode()) {
                System.out.println("✅ 商品向量集合已存在，无需创建");
                return;
            }

            // 2. 创建集合
            // 定义字段：商品ID（主键）、商品文本、向量字段
            FieldType idField = FieldType.newBuilder()
                    .withName("goods_id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType textField = FieldType.newBuilder()
                    .withName("goods_text")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(4096)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName("goods_vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(VECTOR_DIMENSION)
                    .build();

            // 创建集合参数
            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(GOODS_COLLECTION_NAME)
                    .addFieldType(idField)
                    .addFieldType(textField)
                    .addFieldType(vectorField)
                    .withShardsNum(1)
                    .build();

            R<RpcStatus> createResponse = client.createCollection(createParam);
            if (createResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("❌ 创建商品向量集合失败：" + createResponse.getMessage());
            }

            // 3. 创建向量索引
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(GOODS_COLLECTION_NAME)
                    .withFieldName("goods_vector")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\": 1024}")
                    .build();

            R<RpcStatus> indexResponse = client.createIndex(indexParam);
            if (indexResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("❌ 创建向量索引失败：" + indexResponse.getMessage());
            }

            // 4. 加载集合到内存
            LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                    .withCollectionName(GOODS_COLLECTION_NAME)
                    .build();

            R<RpcStatus> loadResponse = client.loadCollection(loadParam);
            if (loadResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("❌ 加载集合失败：" + loadResponse.getMessage());
            }

            System.out.println("✅ 商品向量集合初始化完成");

        } catch (Exception e) {
            throw new RuntimeException("Milvus 集合初始化异常", e);
        }
    }

    /**
     * 【新增】检查并创建校园知识图谱向量集合（复用商品集合的逻辑结构）
     */
    public void initKgCollection() {
        try {
            MilvusServiceClient client = MilvusClientUtil.getClient();

            // 1. 检查集合是否已存在
            R<DescribeCollectionResponse> descResponse = client.describeCollection(
                    DescribeCollectionParam.newBuilder()
                            .withCollectionName(KG_COLLECTION_NAME)
                            .build()
            );

            if (descResponse.getStatus() == R.Status.Success.getCode()) {
                System.out.println("✅ 校园知识图谱向量集合已存在，无需创建");
                return;
            }

            // 2. 创建集合（字段适配 campus_knowledge_graph 表）
            FieldType idField = FieldType.newBuilder()
                    .withName("kg_id")
                    .withDataType(DataType.Int64)
                    .withPrimaryKey(true)
                    .withAutoID(false)
                    .build();

            FieldType sceneTypeField = FieldType.newBuilder()
                    .withName("scene_type")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(32)
                    .build();

            FieldType sceneValueField = FieldType.newBuilder()
                    .withName("scene_value")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(64)
                    .build();

            FieldType categoryIdsField = FieldType.newBuilder()
                    .withName("category_ids")
                    .withDataType(DataType.VarChar)
                    .withMaxLength(128)
                    .build();

            FieldType weightField = FieldType.newBuilder()
                    .withName("weight")
                    .withDataType(DataType.Float)
                    .build();

            FieldType vectorField = FieldType.newBuilder()
                    .withName("kg_vector")
                    .withDataType(DataType.FloatVector)
                    .withDimension(VECTOR_DIMENSION)
                    .build();

            // 创建集合参数（与商品集合保持一致）
            CreateCollectionParam createParam = CreateCollectionParam.newBuilder()
                    .withCollectionName(KG_COLLECTION_NAME)
                    .addFieldType(idField)
                    .addFieldType(sceneTypeField)
                    .addFieldType(sceneValueField)
                    .addFieldType(categoryIdsField)
                    .addFieldType(weightField)
                    .addFieldType(vectorField)
                    .withShardsNum(1)
                    .build();

            R<RpcStatus> createResponse = client.createCollection(createParam);
            if (createResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("❌ 创建校园知识图谱向量集合失败：" + createResponse.getMessage());
            }

            // 3. 创建向量索引（与商品向量索引参数一致）
            CreateIndexParam indexParam = CreateIndexParam.newBuilder()
                    .withCollectionName(KG_COLLECTION_NAME)
                    .withFieldName("kg_vector")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\": 1024}")
                    .build();

            R<RpcStatus> indexResponse = client.createIndex(indexParam);
            if (indexResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("❌ 创建校园知识图谱向量索引失败：" + indexResponse.getMessage());
            }

            // 4. 加载集合到内存
            LoadCollectionParam loadParam = LoadCollectionParam.newBuilder()
                    .withCollectionName(KG_COLLECTION_NAME)
                    .build();

            R<RpcStatus> loadResponse = client.loadCollection(loadParam);
            if (loadResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("❌ 加载校园知识图谱集合失败：" + loadResponse.getMessage());
            }

            System.out.println("✅ 校园知识图谱向量集合初始化完成");

        } catch (Exception e) {
            throw new RuntimeException("Milvus 校园知识图谱集合初始化异常", e);
        }
    }
}