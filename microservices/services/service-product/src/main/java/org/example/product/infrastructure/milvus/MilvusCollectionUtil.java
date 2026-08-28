package org.example.product.infrastructure.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.DataType;
import io.milvus.grpc.DescribeCollectionResponse;
import io.milvus.param.IndexType;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.RpcStatus;
import io.milvus.param.collection.*;
import io.milvus.param.index.CreateIndexParam;
import org.springframework.stereotype.Component;

/**
 * Milvus 集合管理工具类（从 service-main 迁移）
 */
@Component
public class MilvusCollectionUtil {

    public static final String GOODS_COLLECTION_NAME = "second_hand_goods_embedding";
    public static final int VECTOR_DIMENSION = 1024;

    public void initGoodsCollection() {
        try {
            MilvusServiceClient client = MilvusClientUtil.getClient();
            R<DescribeCollectionResponse> descResponse = client.describeCollection(
                    DescribeCollectionParam.newBuilder()
                            .withCollectionName(GOODS_COLLECTION_NAME).build());
            if (descResponse.getStatus() == R.Status.Success.getCode()) {
                return;
            }

            FieldType idField = FieldType.newBuilder()
                    .withName("goods_id").withDataType(DataType.Int64)
                    .withPrimaryKey(true).withAutoID(false).build();
            FieldType textField = FieldType.newBuilder()
                    .withName("goods_text").withDataType(DataType.VarChar)
                    .withMaxLength(4096).build();
            FieldType vectorField = FieldType.newBuilder()
                    .withName("goods_vector").withDataType(DataType.FloatVector)
                    .withDimension(VECTOR_DIMENSION).build();

            R<RpcStatus> createResponse = client.createCollection(
                    CreateCollectionParam.newBuilder()
                            .withCollectionName(GOODS_COLLECTION_NAME)
                            .addFieldType(idField).addFieldType(textField).addFieldType(vectorField)
                            .withShardsNum(1).build());
            if (createResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("创建商品向量集合失败：" + createResponse.getMessage());
            }

            client.createIndex(CreateIndexParam.newBuilder()
                    .withCollectionName(GOODS_COLLECTION_NAME)
                    .withFieldName("goods_vector")
                    .withIndexType(IndexType.IVF_FLAT)
                    .withMetricType(MetricType.COSINE)
                    .withExtraParam("{\"nlist\": 1024}").build());

            client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(GOODS_COLLECTION_NAME).build());
        } catch (Exception e) {
            throw new RuntimeException("Milvus 商品集合初始化异常", e);
        }
    }

}
