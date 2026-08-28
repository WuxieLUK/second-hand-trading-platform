package org.example.product.service;

import org.example.common.PageBean;
import org.example.goods.DTO.GoodsDTO;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.POJO.Goods;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.example.user.VO.BuyerViewSellerVO;

import java.util.List;

public interface ProductDomainService {
    void add(GoodsDTO goodsDTO);

    PageBean<GoodsVO> list(GoodsQueryDTO queryDTO);

    GoodsDetailVO findById(Integer id);

    void update(GoodsDTO goodsDTO);

    void delete(Integer id);

    void updateStatus(Integer id, Integer status);

    BuyerViewSellerVO findSellerByUserId(Integer id);

    PageBean<GoodsVO> alllist(GoodsQueryDTO queryDTO);

    List<GoodsVO> listByIds(List<Integer> ids);

    /**
     * 更新商品库存（供 service-order 通过内部 API 调用）
     * @param id 商品ID
     * @param stockDelta 库存变化量（正数增加，负数减少）
     */
    void updateStock(Integer id, Integer stockDelta);

    /**
     * RAG 向量搜索商品（返回商品列表）
     */
    List<GoodsVO> ragSearch(String query);

    /**
     * RAG 向量搜索 + AI 摘要（一站式接口）
     * @return Map 包含 goodsList（商品列表）和 summary（AI 生成的自然语言总结）
     */
    java.util.Map<String, Object> ragSearchWithSummary(String query);

    /**
     * 查询所有在售商品（供 service-order 库存初始化）
     */
    List<Goods> findAllOnSale();

    /**
     * SQL 搜索商品（主搜索路径，零外部依赖）
     * 支持 keyword 模糊匹配、分类筛选、价格区间、服务端排序、分页
     * 由 Sentinel 熔断保护，异常时返回空分页
     */
    PageBean<GoodsVO> search(GoodsQueryDTO queryDTO);

    /**
     * 协同过滤推荐：根据用户最近收藏的商品，推荐相似用户群体也喜欢的商品
     * @param userId 用户 ID（为 null 时返回热门前 10）
     * @param limit  返回数量上限
     */
    List<GoodsVO> collaborativeRecommend(Integer userId, int limit);

    /**
     * 按分类关键词推荐商品：找到匹配的分类 → 该分类下热门商品 + 同分类协同过滤
     * @param keyword 分类关键词（如"手机"、"服饰"）
     * @param userId  用户 ID（可 null）
     * @param limit   返回数量上限
     */
    List<GoodsVO> recommendByCategory(String keyword, Integer userId, int limit);
}
