package org.example.product.infrastructure.milvus;

import io.milvus.client.MilvusServiceClient;
import io.milvus.grpc.MutationResult;
import io.milvus.param.MetricType;
import io.milvus.param.R;
import io.milvus.param.collection.FlushParam;
import io.milvus.param.collection.LoadCollectionParam;
import io.milvus.param.dml.DeleteParam;
import io.milvus.param.dml.InsertParam;
import io.milvus.param.dml.SearchParam;
import io.milvus.response.SearchResultsWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Milvus 向量操作工具类（从 service-main 迁移）
 */
@Slf4j
@Component
public class MilvusVectorUtil {

    public void insertGoodsVector(Long goodsId, String goodsText, List<Double> vector) {
        try {
            MilvusServiceClient client = MilvusClientUtil.getClient();
            client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME).build());

            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("goods_id", List.of(goodsId)));
            fields.add(new InsertParam.Field("goods_text", List.of(goodsText)));
            List<Float> floatVector = vector.stream().map(Double::floatValue).toList();
            fields.add(new InsertParam.Field("goods_vector", List.of(floatVector)));

            R<MutationResult> response = client.insert(InsertParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .withFields(fields).build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("插入商品向量失败：" + response.getMessage());
            }
            client.flush(FlushParam.newBuilder()
                    .addCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME).build());
        } catch (Exception e) {
            log.error("插入商品向量异常", e);
            throw new RuntimeException("插入商品向量异常", e);
        }
    }

    public List<Long> searchSimilarGoods(List<Double> queryVector, int topK) {
        try {
            MilvusServiceClient client = MilvusClientUtil.getClient();
            List<Float> floatVector = queryVector.stream().map(Double::floatValue).toList();

            R<io.milvus.grpc.SearchResults> response = client.search(SearchParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .withVectorFieldName("goods_vector")
                    .withVectors(List.of(floatVector))
                    .withTopK(topK)
                    .withMetricType(MetricType.COSINE)
                    .withParams("{\"nprobe\": 10}")
                    .withOutFields(List.of("goods_id", "goods_text"))
                    .build());

            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("向量检索失败：" + response.getMessage());
            }

            SearchResultsWrapper resultsWrapper = new SearchResultsWrapper(response.getData().getResults());
            List<Long> goodsIds = new ArrayList<>();
            double threshold = 0.5;
            for (SearchResultsWrapper.IDScore idScore : resultsWrapper.getIDScore(0)) {
                if (idScore.getScore() >= threshold) {
                    goodsIds.add(idScore.getLongID());
                }
            }
            return goodsIds;
        } catch (Exception e) {
            log.error("向量检索异常", e);
            throw new RuntimeException("向量检索异常", e);
        }
    }

    public void deleteGoodsVector(Long goodsId) {
        try {
            MilvusServiceClient client = MilvusClientUtil.getClient();
            String expr = String.format("goods_id in [%d]", goodsId);
            R<MutationResult> response = client.delete(DeleteParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .withExpr(expr).build());
            if (response.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("删除商品向量失败：" + response.getMessage());
            }
            client.flush(FlushParam.newBuilder()
                    .addCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME).build());
        } catch (Exception e) {
            log.error("删除商品向量异常", e);
            throw new RuntimeException("删除商品向量异常", e);
        }
    }
}
