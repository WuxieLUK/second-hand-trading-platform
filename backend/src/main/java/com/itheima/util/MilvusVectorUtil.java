package com.itheima.util;

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
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class MilvusVectorUtil {

    @Autowired
    private MilvusClientUtil milvusClientUtil;

    @Autowired
    private MilvusCollectionUtil milvusCollectionUtil;

    /**
     * 插入商品向量到Milvus（移除is_deleted字段）
     * @param goodsId 商品ID
     * @param goodsText 商品拼接文本（名称+描述）
     * @param vector 商品文本的Embedding向量
     */
    public void insertGoodsVector(Long goodsId, String goodsText, List<Double> vector) {
        try {
            MilvusServiceClient client = milvusClientUtil.getClient();
            // 确保集合已加载
            client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .build());

            // 构造插入数据（仅3个字段，无is_deleted）
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("goods_id", List.of(goodsId)));
            fields.add(new InsertParam.Field("goods_text", List.of(goodsText)));
            // 转换Double向量为Float（Milvus FloatVector要求）
            List<Float> floatVector = vector.stream().map(Double::floatValue).toList();
            fields.add(new InsertParam.Field("goods_vector", List.of(floatVector)));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .withFields(fields)
                    .build();

            R<MutationResult> insertResponse = client.insert(insertParam);
            if (insertResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("插入商品向量失败：" + insertResponse.getMessage());
            }
            // 刷盘确保数据可见
            client.flush(FlushParam.newBuilder()
                    .addCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .build());
            log.info("商品ID:{} 向量插入成功", goodsId);
        } catch (Exception e) {
            log.error("插入商品向量异常", e);
            throw new RuntimeException("插入商品向量异常", e);
        }
    }

    /**
     * 向量检索相似商品（仅保留相似度阈值过滤）
     * @param queryVector 查询文本的 Embedding 向量
     * @param topK 返回相似商品数量
     * @return 相似商品 ID 列表（按相似度降序，仅保留≥0.6的商品）
     */
    public List<Long> searchSimilarGoods(List<Double> queryVector, int topK) {
        try {
            MilvusServiceClient client = milvusClientUtil.getClient();
            // 转换查询向量为 Float
            List<Float> floatVector = queryVector.stream().map(Double::floatValue).toList();

            // 构造检索参数（无is_deleted过滤）
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .withVectorFieldName("goods_vector")
                    .withVectors(List.of(floatVector))
                    .withTopK(topK)
                    .withMetricType(io.milvus.param.MetricType.COSINE)
                    .withParams("{\"nprobe\": 10}")
                    .withOutFields(List.of("goods_id", "goods_text"))
                    .build();

            R<io.milvus.grpc.SearchResults> searchResponse = client.search(searchParam);
            if (searchResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("向量检索失败：" + searchResponse.getMessage());
            }

            // 解析结果 + 过滤低相似度商品
            SearchResultsWrapper resultsWrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
            List<Long> goodsIds = new ArrayList<>();
            List<SearchResultsWrapper.IDScore> scoreList = resultsWrapper.getIDScore(0);

            // 相似度阈值：仅保留≥0.6的商品
            double SIMILARITY_THRESHOLD = 0.5;
            for (SearchResultsWrapper.IDScore idScore : scoreList) {
                double similarity = idScore.getScore();
                Long goodsId = idScore.getLongID();
                log.info("相似商品 ID:{} 相似度:{}", goodsId, similarity);

                if (similarity >= SIMILARITY_THRESHOLD) {
                    goodsIds.add(goodsId);
                } else {
                    log.warn("商品ID:{} 相似度{}低于阈值{}，过滤", goodsId, similarity, SIMILARITY_THRESHOLD);
                }
            }

            return goodsIds;
        } catch (Exception e) {
            log.error("向量检索异常", e);
            throw new RuntimeException("向量检索异常", e);
        }
    }

    /**
     * 根据商品ID删除Milvus中的向量数据（物理删除）
     * @param goodsId 商品ID
     */
    public void deleteGoodsVector(Long goodsId) {
        try {
            MilvusServiceClient client = milvusClientUtil.getClient();

            // 构造删除条件：goods_id in [目标商品 ID]（使用数组语法避免解析错误）
            String deleteExpr = String.format("goods_id in [%d]", goodsId);

            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .withExpr(deleteExpr)
                    .build();

            R<MutationResult> deleteResponse = client.delete(deleteParam);
            if (deleteResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("删除商品向量失败：" + deleteResponse.getMessage());
            }

            // 刷盘确保删除生效
            client.flush(FlushParam.newBuilder()
                    .addCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .build());

            log.info("商品 ID:{} 向量数据已从 Milvus 物理删除", goodsId);
        } catch (Exception e) {
            log.error("删除商品 ID:{} 向量数据异常", goodsId, e);
            throw new RuntimeException("商品删除成功，但向量数据清理失败：" + e.getMessage());
        }
    }

    /**
     * 批量清理Milvus中的脏数据（全量校验）
     * @param validGoodsIds 数据库中仍有效的商品ID列表
     */
    public void cleanDirtyVectorData(List<Long> validGoodsIds) {
        try {
            MilvusServiceClient client = milvusClientUtil.getClient();

            if (validGoodsIds.isEmpty()) {
                log.warn("有效商品ID列表为空，跳过Milvus脏数据清理");
                return;
            }

            // 构造删除条件：goods_id 不在有效ID列表中
            StringBuilder expr = new StringBuilder("goods_id not in [");
            for (int i = 0; i < validGoodsIds.size(); i++) {
                expr.append(validGoodsIds.get(i));
                if (i < validGoodsIds.size() - 1) {
                    expr.append(",");
                }
            }
            expr.append("]");

            DeleteParam deleteParam = DeleteParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                    .withExpr(expr.toString())
                    .build();

            R<MutationResult> deleteResponse = client.delete(deleteParam);
            if (deleteResponse.getStatus() == R.Status.Success.getCode()) {
                client.flush(FlushParam.newBuilder()
                        .addCollectionName(MilvusCollectionUtil.GOODS_COLLECTION_NAME)
                        .build());
                log.info("Milvus脏数据清理完成，删除条件：{}", expr);
            } else {
                log.error("Milvus脏数据清理失败：{}", deleteResponse.getMessage());
            }
        } catch (Exception e) {
            log.error("Milvus脏数据清理异常", e);
        }
    }

    // ========== 【新增】知识图谱向量操作方法 ==========

    /**
     * 插入知识图谱规则向量到Milvus
     * @param kgId 规则ID（campus_knowledge_graph.id）
     * @param sceneType 场景类型（major/grade/scene）
     * @param sceneValue 场景值（如“计算机”“大四”）
     * @param categoryIds 关联分类ID（逗号分隔）
     * @param weight 权重
     * @param vector 1024维向量
     */
    public void insertKgVector(Long kgId, String sceneType, String sceneValue, String categoryIds, Double weight, List<Double> vector) {
        try {
            MilvusServiceClient client = milvusClientUtil.getClient();

            // 修复核心：插入前先初始化/检查集合
            milvusCollectionUtil.initKgCollection();

            // 确保集合已加载
            client.loadCollection(LoadCollectionParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.KG_COLLECTION_NAME)
                    .build());

            // 构造插入数据（与知识图谱集合字段一一对应）
            List<InsertParam.Field> fields = new ArrayList<>();
            fields.add(new InsertParam.Field("kg_id", List.of(kgId)));
            fields.add(new InsertParam.Field("scene_type", List.of(sceneType)));
            fields.add(new InsertParam.Field("scene_value", List.of(sceneValue)));
            fields.add(new InsertParam.Field("category_ids", List.of(categoryIds)));
            fields.add(new InsertParam.Field("weight", List.of(weight.floatValue())));
            // 转换Double向量为Float
            List<Float> floatVector = vector.stream().map(Double::floatValue).toList();
            fields.add(new InsertParam.Field("kg_vector", List.of(floatVector)));

            InsertParam insertParam = InsertParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.KG_COLLECTION_NAME)
                    .withFields(fields)
                    .build();

            R<MutationResult> insertResponse = client.insert(insertParam);
            if (insertResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("插入知识图谱向量失败：" + insertResponse.getMessage());
            }
            // 刷盘确保数据可见
            client.flush(FlushParam.newBuilder()
                    .addCollectionName(MilvusCollectionUtil.KG_COLLECTION_NAME)
                    .build());
            log.info("知识图谱规则ID:{} 向量插入成功", kgId);
        } catch (Exception e) {
            log.error("插入知识图谱向量异常", e);
            throw new RuntimeException("插入知识图谱向量异常", e);
        }
    }

    /**
     * 检索匹配的知识图谱规则（融合用户标签）
     * @param userVector 用户复合向量（major+grade+scene+tags）
     * @param topK 返回前K条匹配结果
     * @return 匹配的分类ID列表（去重，按权重排序）
     */
    public List<Integer> searchKgCategory(List<Double> userVector, int topK) {
        try {
            MilvusServiceClient client = milvusClientUtil.getClient();
            // 转换查询向量为 Float
            List<Float> floatVector = userVector.stream().map(Double::floatValue).toList();

            // 构造检索参数
            SearchParam searchParam = SearchParam.newBuilder()
                    .withCollectionName(MilvusCollectionUtil.KG_COLLECTION_NAME)
                    .withVectorFieldName("kg_vector")
                    .withVectors(List.of(floatVector))
                    .withTopK(topK)
                    .withMetricType(MetricType.COSINE)
                    .withParams("{\"nprobe\": 10}")
                    .withOutFields(List.of("category_ids", "weight")) // 返回分类ID和权重
                    .build();

            R<io.milvus.grpc.SearchResults> searchResponse = client.search(searchParam);
            if (searchResponse.getStatus() != R.Status.Success.getCode()) {
                throw new RuntimeException("知识图谱向量检索失败：" + searchResponse.getMessage());
            }

            // 解析结果 + 按权重过滤/排序
            SearchResultsWrapper resultsWrapper = new SearchResultsWrapper(searchResponse.getData().getResults());
            List<Integer> categoryIds = new ArrayList<>();
            List<SearchResultsWrapper.IDScore> scoreList = resultsWrapper.getIDScore(0);

            // 相似度阈值：仅保留≥0.5的规则
            double SIMILARITY_THRESHOLD = 0.5;
            for (SearchResultsWrapper.IDScore idScore : scoreList) {
                double similarity = idScore.getScore();
                if (similarity < SIMILARITY_THRESHOLD) {
                    continue;
                }

                // 解析分类ID
                String categoryStr = (String) idScore.getFieldValues().get("category_ids");
                if (categoryStr != null && !categoryStr.isEmpty()) {
                    List<Integer> ids = Arrays.stream(categoryStr.split(","))
                            .map(String::trim)
                            .map(Integer::parseInt)
                            .collect(Collectors.toList());
                    categoryIds.addAll(ids);
                }
            }

            // 去重并返回
            return categoryIds.stream().distinct().collect(Collectors.toList());
        } catch (Exception e) {
            log.error("知识图谱向量检索异常", e);
            throw new RuntimeException("知识图谱向量检索异常", e);
        }
    }
}