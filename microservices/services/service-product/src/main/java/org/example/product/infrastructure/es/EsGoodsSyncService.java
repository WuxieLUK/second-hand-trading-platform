package org.example.product.infrastructure.es;

import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.example.goods.POJO.Goods;
import org.example.goods.VO.GoodsVO;
import org.example.product.mapper.GoodsCollectMapper;
import org.example.product.mapper.GoodsMapper;
import org.example.product.mapper.ShopCategoryMapper;
import org.springframework.data.elasticsearch.core.ElasticsearchOperations;
import org.springframework.data.elasticsearch.core.IndexOperations;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

/**
 * ES 商品索引同步服务
 * 启动时全量同步 + 提供增量同步接口
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class EsGoodsSyncService {

    private final GoodsMapper goodsMapper;
    private final ShopCategoryMapper shopCategoryMapper;
    private final GoodsCollectMapper goodsCollectMapper;
    private final GoodsEsRepository goodsEsRepository;
    private final ElasticsearchOperations elasticsearchOperations;

    /**
     * 启动时自动创建索引并全量同步
     */
    @PostConstruct
    public void init() {
        try {
            IndexOperations indexOps = elasticsearchOperations.indexOps(GoodsDocument.class);
            if (!indexOps.exists()) {
                indexOps.create();
                indexOps.putMapping(indexOps.createMapping());
                log.info("ES 索引 [goods] 创建成功");
            }
            fullSync();
        } catch (Exception e) {
            log.warn("ES 初始化失败（ES 可能未启动），搜索将降级到 SQL: {}", e.getMessage());
        }
    }

    /**
     * 全量同步：将所有在售商品写入 ES
     */
    public void fullSync() {
        try {
            List<Goods> allGoods = goodsMapper.findAllOnSale();
            if (allGoods.isEmpty()) {
                log.info("ES 全量同步：无在售商品");
                return;
            }
            List<GoodsDocument> docs = allGoods.stream()
                    .map(this::toDocument)
                    .collect(Collectors.toList());
            goodsEsRepository.saveAll(docs);
            log.info("ES 全量同步完成，共 {} 条", docs.size());
        } catch (Exception e) {
            log.error("ES 全量同步失败", e);
        }
    }

    /**
     * 增量同步：单个商品
     */
    public void syncGoods(Integer goodsId) {
        try {
            Goods goods = goodsMapper.findById(goodsId);
            if (goods == null) return;
            GoodsDocument doc = toDocument(goods);
            goodsEsRepository.save(doc);
            log.debug("ES 同步商品 {} 完成", goodsId);
        } catch (Exception e) {
            log.error("ES 同步商品 {} 失败", goodsId, e);
        }
    }

    /**
     * 删除 ES 中的商品
     */
    public void deleteGoods(Integer goodsId) {
        try {
            goodsEsRepository.deleteById(goodsId);
            log.debug("ES 删除商品 {} 完成", goodsId);
        } catch (Exception e) {
            log.error("ES 删除商品 {} 失败", goodsId, e);
        }
    }

    /**
     * 更新 ES 中的行为计数（viewCount / collectCount / orderCount）
     */
    public void syncBehaviorCounts(Integer goodsId) {
        try {
            GoodsDocument doc = goodsEsRepository.findById(goodsId).orElse(null);
            if (doc == null) return;

            // 从 MySQL 查询最新计数
            Goods goods = goodsMapper.findById(goodsId);
            if (goods == null) {
                log.warn("ES 同步行为计数：商品 {} 不存在", goodsId);
                return;
            }
            doc.setViewCount(goods.getViewCount() != null ? goods.getViewCount() : 0);
            doc.setCollectCount(goodsCollectMapper.allList(goodsId));
            goodsEsRepository.save(doc);
        } catch (Exception e) {
            log.error("ES 同步行为计数失败 goodsId={}", goodsId, e);
        }
    }

    /**
     * 商品转 ES 文档
     */
    private GoodsDocument toDocument(Goods goods) {
        GoodsDocument doc = new GoodsDocument();
        doc.setId(goods.getId());
        doc.setGoodsName(goods.getGoodsName());
        doc.setGoodsDesc(goods.getGoodsDesc());
        doc.setGoodsPic(goods.getGoodsPic());
        doc.setCategoryId(goods.getCategoryId());
        doc.setSellPrice(goods.getSellPrice());
        doc.setIsNew(goods.getIsNew());
        doc.setGoodsStatus(goods.getGoodsStatus());
        doc.setSellerId(goods.getSellerId());
        doc.setCreateTime(goods.getCreateTime());

        // 分类名称
        if (goods.getCategoryId() != null) {
            var category = shopCategoryMapper.findById(goods.getCategoryId());
            if (category != null) {
                doc.setCategoryName(category.getCategoryName());
            }
        }

        // 行为计数
        try {
            doc.setViewCount(goods.getViewCount() != null ? goods.getViewCount() : 0);
            doc.setCollectCount(goodsCollectMapper.allList(goods.getId()));
        } catch (Exception e) {
            doc.setViewCount(0);
            doc.setCollectCount(0);
        }

        return doc;
    }
}
