package com.itheima.service;

import org.example.goods.DTO.GoodsDTO;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.common.PageBean;
import org.example.user.VO.BuyerViewSellerVO;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;

import java.util.List;
import java.util.Map;

/**
 * 商品服务接口（合并完整版）
 */
public interface GoodsService {

    // ==================== 基础 CURD 方法 ====================
    void add(GoodsDTO goodsDTO);

    PageBean<GoodsVO> list(GoodsQueryDTO queryDTO);

    GoodsDetailVO findById(Integer id);

    void update(GoodsDTO goodsDTO);

    void delete(Integer id);

    void updateStatus(Integer id, Integer status);

    BuyerViewSellerVO findSellerByUserId(Integer id);

    PageBean<GoodsVO> alllist(GoodsQueryDTO queryDTO);

    // ==================== 来自第一个Service，新增补充的方法 ====================
    List<GoodsVO> listByIds(List<Integer> ids);

    /**
     * SQL 搜索商品（主搜索路径，零外部依赖）
     */
    PageBean<GoodsVO> search(GoodsQueryDTO queryDTO);

    // ==================== 原有第二个Service独有的方法 ====================
    List<GoodsVO> ragSearch(String query);

    /**
     * RAG 向量搜索 + AI 摘要（一站式，通过 Feign 调用 service-product）
     */
    Map<String, Object> ragSearchWithSummary(String query);

    /**
     * 全量校验并清理Milvus脏数据
     */
    void cleanMilvusDirtyData();

    /**
     * 协同过滤推荐：根据用户收藏历史推荐商品
     * @param userId 用户 ID（null 时返回热门商品）
     * @param limit  返回数量上限
     */
    List<GoodsVO> collaborativeRecommend(Integer userId, int limit);

    /**
     * 按分类关键词推荐：匹配分类 → 该分类下热门商品 + 同分类协同过滤
     * @param keyword 分类关键词（如"手机"、"服饰"）
     * @param userId  用户 ID（可 null）
     * @param limit   返回数量上限
     */
    List<GoodsVO> recommendByCategory(String keyword, Integer userId, int limit);
}
