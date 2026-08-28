package org.example.product.infrastructure.es;

import org.springframework.data.elasticsearch.repository.ElasticsearchRepository;
import org.springframework.stereotype.Repository;

/**
 * ES 商品索引 Repository
 * 提供基本的 CRUD 和搜索操作
 */
@Repository
public interface GoodsEsRepository extends ElasticsearchRepository<GoodsDocument, Integer> {

    /**
     * 按商品状态查询
     */
    Iterable<GoodsDocument> findByGoodsStatus(Integer goodsStatus);
}
