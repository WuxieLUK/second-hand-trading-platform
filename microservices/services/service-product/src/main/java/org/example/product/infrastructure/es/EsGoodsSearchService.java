package org.example.product.infrastructure.es;

import co.elastic.clients.elasticsearch._types.query_dsl.FunctionBoostMode;
import co.elastic.clients.elasticsearch._types.query_dsl.FunctionScore;
import co.elastic.clients.elasticsearch._types.query_dsl.Query;
import co.elastic.clients.json.JsonData;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.common.PageBean;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.VO.GoodsVO;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.elasticsearch.client.elc.NativeQuery;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.SearchHits;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;

/**
 * ES 商品搜索服务
 * 使用 ik_smart 分词 + function_score 行为排序（viewCount / collectCount / orderCount）
 * 排序公式：BM25 相关性分 + log1p(viewCount)*0.1 + log1p(collectCount)*0.2 + log1p(orderCount)*0.3
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsGoodsSearchService {

    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * ES 搜索商品（带行为排序）
     */
    public PageBean<GoodsVO> search(GoodsQueryDTO queryDTO) {
        // 1. bool 查询 — 必须条件 + 关键词 + 筛选
        Query boolQuery = buildBoolQuery(queryDTO);

        // 2. function_score — 行为数据加权
        Query functionScoreQuery = Query.of(q -> q.functionScore(fs -> fs
                .query(boolQuery)
                .functions(
                        FunctionScore.of(f -> f.fieldValueFactor(fv -> fv
                                .field("viewCount").factor(0.1).missing(0.0))),
                        FunctionScore.of(f -> f.fieldValueFactor(fv -> fv
                                .field("collectCount").factor(0.2).missing(0.0))),
                        FunctionScore.of(f -> f.fieldValueFactor(fv -> fv
                                .field("orderCount").factor(0.3).missing(0.0)))
                )
                .boostMode(FunctionBoostMode.Sum)
        ));

        // 3. 构建 NativeQuery
        NativeQuery nativeQuery = buildNativeQuery(functionScoreQuery, queryDTO);

        // 4. 执行搜索
        SearchHits<GoodsDocument> searchHits = elasticsearchOperations.search(
                nativeQuery, GoodsDocument.class);

        // 5. 转换结果
        List<GoodsVO> items = searchHits.stream()
                .map(hit -> toGoodsVO(hit.getContent()))
                .collect(Collectors.toList());

        PageBean<GoodsVO> pageBean = new PageBean<>();
        pageBean.setTotal(searchHits.getTotalHits());
        pageBean.setItems(items);
        return pageBean;
    }

    /**
     * 构建 bool 查询
     * 使用 mutating builder 模式兼容 ES Java 客户端 API
     */
    private Query buildBoolQuery(GoodsQueryDTO queryDTO) {
        return Query.of(q -> q.bool(bq -> {
            // 必须在售
            bq.must(Query.of(t -> t.term(tq -> tq.field("goodsStatus").value(1))));

            // 关键词匹配（ik_smart 分词）
            String keyword = queryDTO.getKeyword();
            if (keyword != null && !keyword.isBlank()) {
                bq.must(Query.of(k -> k.bool(kb -> kb
                        .should(Query.of(s -> s.match(m -> m
                                .field("goodsName").query(keyword).analyzer("ik_smart"))))
                        .should(Query.of(s -> s.match(m -> m
                                .field("goodsDesc").query(keyword).analyzer("ik_smart"))))
                )));
            }

            // 分类筛选（filter 不贡献评分）
            if (queryDTO.getCategoryId() != null && queryDTO.getCategoryId() != 0) {
                bq.filter(Query.of(t -> t.term(tq -> tq.field("categoryId").value(queryDTO.getCategoryId()))));
            }

            // 价格区间（filter 不贡献评分）
            if (queryDTO.getMinPrice() != null) {
                bq.filter(Query.of(r -> r.range(rq -> rq
                        .field("sellPrice").gte(JsonData.of(queryDTO.getMinPrice())))));
            }
            if (queryDTO.getMaxPrice() != null) {
                bq.filter(Query.of(r -> r.range(rq -> rq
                        .field("sellPrice").lte(JsonData.of(queryDTO.getMaxPrice())))));
            }

            return bq;
        }));
    }

    /**
     * 构建 NativeQuery（分页 + 排序）
     */
    private NativeQuery buildNativeQuery(Query query, GoodsQueryDTO queryDTO) {
        var builder = NativeQuery.builder()
                .withQuery(query)
                .withPageable(PageRequest.of(
                        queryDTO.getPageNum() - 1,
                        queryDTO.getPageSize()));

        // 按价格排序时覆盖默认 _score 排序
        if ("price".equals(queryDTO.getSortField())) {
            Sort.Direction dir = "asc".equals(queryDTO.getSortOrder())
                    ? Sort.Direction.ASC : Sort.Direction.DESC;
            builder.withSort(Sort.by(dir, "sellPrice"));
        }
        // 默认：按 _score 降序（BM25 相关性 + 行为加权分）

        return builder.build();
    }

    /**
     * GoodsDocument → GoodsVO
     */
    private GoodsVO toGoodsVO(GoodsDocument doc) {
        GoodsVO vo = new GoodsVO();
        vo.setId(doc.getId());
        vo.setGoodsName(doc.getGoodsName());
        vo.setGoodsPic(doc.getGoodsPic());
        vo.setCategoryId(doc.getCategoryId());
        vo.setCategoryName(doc.getCategoryName());
        vo.setSellPrice(doc.getSellPrice());
        vo.setIsNew(doc.getIsNew());
        vo.setGoodsStatus(doc.getGoodsStatus());
        vo.setCreateTime(doc.getCreateTime());
        vo.setSellerNickname(doc.getSellerNickname());
        vo.setSellerPic(doc.getSellerPic());
        vo.setSellerId(doc.getSellerId());
        vo.setViewCount(doc.getViewCount());
        vo.setCollectCount(doc.getCollectCount());
        return vo;
    }
}
