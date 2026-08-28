package org.example.product.service.impl;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import lombok.extern.slf4j.Slf4j;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.product.exception.SentinelBlockHandler;
import org.example.product.feign.UserFeignClient;
import org.example.goods.DTO.GoodsDTO;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.POJO.Goods;
import org.example.goods.POJO.GoodsImage;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.example.product.constant.GoodsIsNewEnum;
import org.example.product.constant.GoodsStatusEnum;
import org.example.product.infrastructure.es.EsGoodsSearchService;
import org.example.product.infrastructure.es.EsGoodsSyncService;
import org.example.product.mapper.GoodsCollectMapper;
import org.example.product.mapper.GoodsMapper;
import org.example.product.mapper.ShopCategoryMapper;
import org.example.product.service.ProductDomainService;
import org.example.user.POJO.User;
import org.example.user.VO.BuyerViewSellerVO;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * 商品域服务实现
 * 注意：不再直接访问 user 和 order 数据库，改为通过 Feign 调用对应服务
 */
@Slf4j
@Service
public class ProductDomainServiceImpl implements ProductDomainService {

    private final GoodsMapper goodsMapper;
    private final ShopCategoryMapper shopCategoryMapper;
    private final GoodsCollectMapper goodsCollectMapper;
    private final UserFeignClient userFeignClient;
    private final EsGoodsSearchService esGoodsSearchService;
    private final EsGoodsSyncService esGoodsSyncService;
    private final org.example.product.infrastructure.ai.QwenEmbeddingUtil qwenEmbeddingUtil;
    private final org.example.product.infrastructure.milvus.MilvusVectorUtil milvusVectorUtil;
    private final org.example.product.infrastructure.ai.QwenChatUtil qwenChatUtil;

    public ProductDomainServiceImpl(GoodsMapper goodsMapper,
                                    ShopCategoryMapper shopCategoryMapper,
                                    GoodsCollectMapper goodsCollectMapper,
                                    UserFeignClient userFeignClient,
                                    EsGoodsSearchService esGoodsSearchService,
                                    EsGoodsSyncService esGoodsSyncService,
                                    org.example.product.infrastructure.ai.QwenEmbeddingUtil qwenEmbeddingUtil,
                                    org.example.product.infrastructure.milvus.MilvusVectorUtil milvusVectorUtil,
                                    org.example.product.infrastructure.ai.QwenChatUtil qwenChatUtil) {
        this.goodsMapper = goodsMapper;
        this.shopCategoryMapper = shopCategoryMapper;
        this.goodsCollectMapper = goodsCollectMapper;
        this.userFeignClient = userFeignClient;
        this.esGoodsSearchService = esGoodsSearchService;
        this.esGoodsSyncService = esGoodsSyncService;
        this.qwenEmbeddingUtil = qwenEmbeddingUtil;
        this.milvusVectorUtil = milvusVectorUtil;
        this.qwenChatUtil = qwenChatUtil;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(GoodsDTO goodsDTO) {
        Goods goods = new Goods();
        BeanUtils.copyProperties(goodsDTO, goods);
        LocalDateTime now = LocalDateTime.now();
        goods.setCreateTime(now);
        goods.setUpdateTime(now);
        goods.setGoodsStatus(resolveGoodsStatus(goods.getStock(), goods.getGoodsStatus()));
        goodsMapper.add(goods);
        List<GoodsImage> imageList = goodsDTO.getImageList();
        if (imageList != null && !imageList.isEmpty()) {
            goodsMapper.insertGoodsImages(goods.getId(), imageList);
        }
        // 同步到 ES
        esGoodsSyncService.syncGoods(goods.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<GoodsVO> list(GoodsQueryDTO queryDTO) {
        queryDTO.setGoodsStatus(GoodsStatusEnum.ON_SALE.getCode());

        // 猜你喜欢（无分类过滤）→ 协同过滤 + 热门商品混合
        if (queryDTO.getCategoryId() == null && queryDTO.getRecommendUserId() != null) {
            return recommendListWithPage(queryDTO);
        }

        // 分类查询：如果 categoryId 是父分类，展开为包含所有子分类的 ID 列表
        if (queryDTO.getCategoryId() != null && queryDTO.getCategoryId() != 0) {
            List<Integer> childIds = shopCategoryMapper.listChildCategoryIdsByParentId(queryDTO.getCategoryId());
            if (childIds != null && !childIds.isEmpty()) {
                List<Integer> allIds = new ArrayList<>();
                allIds.add(queryDTO.getCategoryId()); // 包含父分类自身
                allIds.addAll(childIds);
                queryDTO.setCategoryIds(allIds);
                queryDTO.setCategoryId(null); // 交由 categoryIds 过滤
            }
            // 如果没有子分类，则 categoryId 保持原值，按单分类精确过滤
        }
        return alllist(queryDTO);
    }

    /**
     * 猜你喜欢：协同过滤 + 热门兜底，支持分页
     */
    private PageBean<GoodsVO> recommendListWithPage(GoodsQueryDTO queryDTO) {
        int pageNum = queryDTO.getPageNum() != null ? queryDTO.getPageNum() : 1;
        int pageSize = queryDTO.getPageSize() != null ? queryDTO.getPageSize() : 10;
        int fetchLimit = pageNum * pageSize; // 预取足够数量用于分页

        // 1. 协同过滤
        List<GoodsVO> merged = collaborativeRecommend(queryDTO.getRecommendUserId(), fetchLimit);

        // 2. 如果协同过滤结果不够，用热门商品补充
        if (merged.size() < fetchLimit) {
            Set<Integer> seen = new HashSet<>();
            for (GoodsVO g : merged) {
                seen.add(g.getId());
            }
            List<GoodsVO> hotList = getHotGoods(fetchLimit);
            for (GoodsVO g : hotList) {
                if (seen.add(g.getId())) {
                    merged.add(g);
                    if (merged.size() >= fetchLimit) break;
                }
            }
        }

        // 3. 分页切片
        int from = (pageNum - 1) * pageSize;
        int to = Math.min(from + pageSize, merged.size());
        List<GoodsVO> pageItems = from < merged.size() ? merged.subList(from, to) : List.of();

        PageBean<GoodsVO> pb = new PageBean<>();
        pb.setItems(pageItems);
        pb.setTotal((long) merged.size());
        return pb;
    }

    @Override
    @Transactional(readOnly = true)
    public GoodsDetailVO findById(Integer id) {
        Goods goods = goodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }

        GoodsDetailVO detailVO = new GoodsDetailVO();
        BeanUtils.copyProperties(goods, detailVO);
        detailVO.setImageList(goodsMapper.findGoodsImagesByGoodsId(id));

        // 折扣计算
        if (goods.getOriginalPrice() != null && goods.getOriginalPrice().compareTo(BigDecimal.ZERO) > 0) {
            BigDecimal discount = goods.getSellPrice().divide(goods.getOriginalPrice(), 2, BigDecimal.ROUND_HALF_UP);
            detailVO.setDiscount(discount.multiply(BigDecimal.TEN) + "折");
        } else {
            detailVO.setDiscount("无折扣");
        }

        // 分类名称（本地数据）
        if (shopCategoryMapper.findById(goods.getCategoryId()) != null) {
            detailVO.setCategoryName(shopCategoryMapper.findById(goods.getCategoryId()).getCategoryName());
        }

        detailVO.setIsNewName(GoodsIsNewEnum.getNameByCode(goods.getIsNew()));
        detailVO.setGoodsStatusName(GoodsStatusEnum.getNameByCode(goods.getGoodsStatus()));

        // 卖家信息从 service-user 获取（通过 Feign）
        User seller = fetchUserById(goods.getSellerId());
        if (seller != null) {
            detailVO.setSellerNickname(seller.getNickname());
            detailVO.setSellerAvatar(seller.getUserPic());
        }

        detailVO.setCollectCount(goodsCollectMapper.allList(id));

        // 记录浏览次数并异步同步到 ES
        detailVO.setViewCount(goodsMapper.getViewCount(id));
        try {
            goodsMapper.incrementViewCount(id);
            esGoodsSyncService.syncBehaviorCounts(id);
        } catch (Exception e) {
            log.warn("记录浏览数失败 goodsId={}", id, e);
        }

        return detailVO;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(GoodsDTO goodsDTO) {
        Goods goods = goodsMapper.findById(goodsDTO.getId());
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        BeanUtils.copyProperties(goodsDTO, goods);
        goods.setGoodsStatus(resolveGoodsStatus(goods.getStock(), goods.getGoodsStatus()));
        goods.setUpdateTime(LocalDateTime.now());
        goodsMapper.update(goods);
        goodsMapper.deleteGoodsImagesByGoodsId(goods.getId());
        if (goodsDTO.getImageList() != null && !goodsDTO.getImageList().isEmpty()) {
            goodsMapper.insertGoodsImages(goods.getId(), goodsDTO.getImageList());
        }
        // 同步到 ES
        esGoodsSyncService.syncGoods(goods.getId());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer id) {
        Goods goods = goodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        goodsMapper.deleteGoodsImagesByGoodsId(id);
        goodsMapper.delete(id);
        // 从 ES 删除
        esGoodsSyncService.deleteGoods(id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStatus(Integer id, Integer status) {
        goodsMapper.updateStatus(id, status, LocalDateTime.now());
    }

    @Override
    @Transactional(readOnly = true)
    public BuyerViewSellerVO findSellerByUserId(Integer id) {
        return goodsMapper.findSellerByUserId(id);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<GoodsVO> alllist(GoodsQueryDTO queryDTO) {
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<GoodsVO> list = goodsMapper.allList(queryDTO);
        for (GoodsVO goodsVO : list) {
            enrichGoodsVO(goodsVO);
        }
        Page<GoodsVO> page = (Page<GoodsVO>) list;
        PageBean<GoodsVO> pageBean = new PageBean<>();
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsVO> listByIds(List<Integer> ids) {
        if (ids == null || ids.isEmpty()) {
            return List.of();
        }
        List<GoodsVO> goodsVOS = goodsMapper.listByIds(ids);
        for (GoodsVO goodsVO : goodsVOS) {
            enrichGoodsVO(goodsVO);
        }
        return goodsVOS;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void updateStock(Integer id, Integer stockDelta) {
        Goods goods = goodsMapper.findById(id);
        if (goods == null) {
            throw new IllegalArgumentException("商品不存在");
        }
        int newStock = goods.getStock() + stockDelta;
        if (newStock < 0) {
            throw new IllegalStateException("库存不足");
        }
        goods.setStock(newStock);
        if (newStock == 0) {
            goods.setGoodsStatus(GoodsStatusEnum.SOLD_OUT.getCode());
        } else if (newStock > 0 && !Objects.equals(goods.getGoodsStatus(), GoodsStatusEnum.ON_SALE.getCode())) {
            goods.setGoodsStatus(GoodsStatusEnum.ON_SALE.getCode());
        }
        goods.setUpdateTime(LocalDateTime.now());
        goodsMapper.update(goods);
    }

    @Override
    @Transactional(readOnly = true)
    @SentinelResource(value = "goodsSearch",
            blockHandlerClass = SentinelBlockHandler.class,
            blockHandler = "goodsSearchBlocked",
            fallback = "searchFallback")
    public PageBean<GoodsVO> search(GoodsQueryDTO queryDTO) {
        // 1. 优先使用 ES 搜索（分词 + 行为排序）
        try {
            PageBean<GoodsVO> esResult = esGoodsSearchService.search(queryDTO);
            if (esResult != null && !esResult.getItems().isEmpty()) {
                log.debug("ES 搜索命中 keyword={}, total={}", queryDTO.getKeyword(), esResult.getTotal());
                // 补充 VO 中的 Feign 字段（卖家昵称等已在 ES 文档中，无需额外查询）
                esResult.getItems().forEach(this::enrichGoodsVOFast);
                return esResult;
            }
        } catch (Exception e) {
            log.warn("ES 搜索失败，降级到 SQL, keyword={}, reason={}", queryDTO.getKeyword(), e.getMessage());
        }

        // 2. ES 降级：使用 SQL 搜索
        PageHelper.startPage(queryDTO.getPageNum(), queryDTO.getPageSize());
        List<GoodsVO> list = goodsMapper.searchByKeyword(queryDTO);
        for (GoodsVO goodsVO : list) {
            enrichGoodsVO(goodsVO);
        }
        Page<GoodsVO> page = (Page<GoodsVO>) list;
        PageBean<GoodsVO> pageBean = new PageBean<>();
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
    }

    /**
     * Sentinel 降级方法：ES / SQL 搜索异常时返回空分页
     */
    @SuppressWarnings("unused")
    public PageBean<GoodsVO> searchFallback(GoodsQueryDTO queryDTO, Throwable t) {
        log.warn("搜索降级（Sentinel 熔断），keyword={}，原因：{}", queryDTO.getKeyword(), t.getMessage());
        PageBean<GoodsVO> pageBean = new PageBean<>();
        pageBean.setTotal(0L);
        pageBean.setItems(List.of());
        return pageBean;
    }

    @Override
    @Transactional(readOnly = true)
    @SentinelResource(value = "ragSearch",
            blockHandlerClass = SentinelBlockHandler.class,
            blockHandler = "ragSearchBlocked",
            fallback = "ragSearchFallback")
    public List<GoodsVO> ragSearch(String query) {
        if (query == null || query.isBlank()) {
            return List.of();
        }
        try {
            // 1. 生成查询文本的向量
            List<Double> queryVector = qwenEmbeddingUtil.getEmbedding(query);

            // 2. 在 Milvus 中搜索相似商品
            List<Long> goodsIds = milvusVectorUtil.searchSimilarGoods(queryVector, 10);

            if (goodsIds.isEmpty()) {
                return List.of();
            }

            // 3. 根据 ID 从数据库查询完整商品信息
            List<Integer> intIds = goodsIds.stream()
                    .map(Long::intValue)
                    .toList();
            List<GoodsVO> goodsList = goodsMapper.listByIds(intIds);

            // 4. 补充 VO 信息
            goodsList.forEach(this::enrichGoodsVO);
            return goodsList;
        } catch (Exception e) {
            log.error("RAG 向量搜索失败, query={}", query, e);
            return List.of();
        }
    }

    /**
     * RAG 搜索 Sentinel 降级：返回空列表，前端降级显示提示
     */
    @SuppressWarnings("unused")
    public List<GoodsVO> ragSearchFallback(String query, Throwable t) {
        log.warn("RAG搜索降级，query={}，原因：{}", query, t.getMessage());
        return List.of();
    }

    @Override
    public java.util.Map<String, Object> ragSearchWithSummary(String query) {
        java.util.Map<String, Object> result = new java.util.HashMap<>();
        List<GoodsVO> goodsList = ragSearch(query);
        result.put("goodsList", goodsList);
        result.put("total", goodsList.size());

        // 生成 AI 摘要
        String summary = qwenChatUtil.generateSearchSummary(query, goodsList);
        result.put("summary", summary);

        return result;
    }

    // ========== 协同过滤 ==========

    @Override
    @Transactional(readOnly = true)
    public List<GoodsVO> recommendByCategory(String keyword, Integer userId, int limit) {
        try {
            // 1. 支持逗号分隔的多关键词，合并所有匹配的分类
            List<Integer> allCids = new ArrayList<>();
            String[] keywords = keyword.split("[,，]");
            for (String kw : keywords) {
                String trimmed = kw.trim();
                if (trimmed.isEmpty()) continue;
                List<Integer> matchedIds = shopCategoryMapper.findCategoryIdsByNameLike(trimmed);
                for (Integer id : matchedIds) {
                    if (!allCids.contains(id)) allCids.add(id);
                    // 展开子分类
                    List<Integer> children = shopCategoryMapper.listChildCategoryIdsByParentId(id);
                    if (children != null) {
                        for (Integer c : children) {
                            if (!allCids.contains(c)) allCids.add(c);
                        }
                    }
                }
            }

            if (allCids.isEmpty()) {
                return List.of();
            }

            // 2. 获取该分类下的热门商品（按浏览次数降序）
            List<GoodsVO> hotList = goodsMapper.findHotByCategoryIds(allCids, limit);
            hotList.forEach(this::enrichGoodsVOFast);

            // 4. 已登录用户有收藏 → 同分类下的协同过滤结果前置
            if (userId != null) {
                List<Integer> collectedIds = goodsCollectMapper.findCollectedGoodsIdsByUserId(userId);
                if (!collectedIds.isEmpty()) {
                    List<GoodsVO> cfAll = collaborativeRecommend(userId, limit * 2);
                    List<GoodsVO> cfInCat = new ArrayList<>();
                    for (GoodsVO g : cfAll) {
                        if (allCids.contains(g.getCategoryId())) {
                            cfInCat.add(g);
                        }
                    }

                    // CF 结果在前，热门商品在后（去重）
                    Set<Integer> seen = new HashSet<>();
                    List<GoodsVO> merged = new ArrayList<>();
                    for (GoodsVO g : cfInCat) {
                        if (seen.add(g.getId())) merged.add(g);
                    }
                    for (GoodsVO g : hotList) {
                        if (seen.add(g.getId())) {
                            merged.add(g);
                            if (merged.size() >= limit) break;
                        }
                    }
                    return merged.size() > limit ? merged.subList(0, limit) : merged;
                }
            }

            return hotList.size() > limit ? hotList.subList(0, limit) : hotList;
        } catch (Exception e) {
            log.error("按分类推荐失败, keyword={}, userId={}", keyword, userId, e);
            return List.of();
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsVO> collaborativeRecommend(Integer userId, int limit) {
        try {
            List<Integer> collectedIds;
            if (userId != null) {
                // 已登录用户：获取其收藏商品
                collectedIds = goodsCollectMapper.findCollectedGoodsIdsByUserId(userId);
            } else {
                collectedIds = List.of();
            }

            if (collectedIds == null || collectedIds.isEmpty()) {
                // 匿名用户或无收藏：返回在售热门商品（按 view_count 降序）
                return getHotGoods(limit);
            }

            // 协同过滤：找"收藏了这些商品的用户也收藏了啥"
            List<java.util.Map<String, Object>> cfResult =
                    goodsCollectMapper.findCollaborativeItems(collectedIds, limit);

            if (cfResult.isEmpty()) {
                return getHotGoods(limit);
            }

            // 提取推荐商品 ID
            List<Integer> recommendIds = cfResult.stream()
                    .map(m -> (Integer) m.get("goodsId"))
                    .filter(Objects::nonNull)
                    .distinct()
                    .limit(limit)
                    .toList();

            if (recommendIds.isEmpty()) {
                return getHotGoods(limit);
            }

            List<GoodsVO> result = goodsMapper.listByIds(recommendIds);
            // 仅保留在售商品
            result.removeIf(vo -> !Objects.equals(vo.getGoodsStatus(), GoodsStatusEnum.ON_SALE.getCode()));
            result.forEach(this::enrichGoodsVO);
            return result;
        } catch (Exception e) {
            log.error("协同过滤推荐失败 userId={}", userId, e);
            return List.of();
        }
    }

    /**
     * 获取热门商品（按浏览次数降序）
     * 注意：不传 limit 给 SQL，取回后在应用层按 viewCount 排序再截取
     */
    private List<GoodsVO> getHotGoods(int limit) {
        try {
            PageHelper.startPage(1, 50); // 取前 50 条做应用层排序
            List<GoodsVO> list = goodsMapper.findAllOnSaleWithSeller();
            // 按 viewCount 降序
            list.sort((a, b) -> {
                int va = a.getViewCount() != null ? a.getViewCount() : 0;
                int vb = b.getViewCount() != null ? b.getViewCount() : 0;
                return Integer.compare(vb, va);
            });
            List<GoodsVO> top = list.subList(0, Math.min(limit, list.size()));
            top.forEach(this::enrichGoodsVOFast);
            return top;
        } catch (Exception e) {
            log.warn("获取热门商品失败", e);
            return List.of();
        }
    }

    // ========== 私有辅助方法 ==========

    /**
     * 轻量级 enrich：ES 搜索结果已含行为计数，只需补充图片、枚举名称和卖家信息
     */
    private void enrichGoodsVOFast(GoodsVO goodsVO) {
        if (goodsVO == null) return;
        goodsVO.setImageList(goodsMapper.findGoodsImagesByGoodsId(goodsVO.getId()));
        goodsVO.setIsNewName(GoodsIsNewEnum.getNameByCode(goodsVO.getIsNew()));
        goodsVO.setGoodsStatusName(GoodsStatusEnum.getNameByCode(goodsVO.getGoodsStatus()));
        // 卖家信息（ES 同步时未存昵称/头像，需 Feign 补充）
        if (goodsVO.getSellerNickname() == null && goodsVO.getSellerId() != null) {
            User seller = fetchUserById(goodsVO.getSellerId());
            if (seller != null) {
                goodsVO.setSellerNickname(seller.getNickname());
                goodsVO.setSellerPic(seller.getUserPic());
            }
        }
    }

    private void enrichGoodsVO(GoodsVO goodsVO) {
        if (goodsVO == null) return;

        goodsVO.setImageList(goodsMapper.findGoodsImagesByGoodsId(goodsVO.getId()));
        goodsVO.setCollectCount(goodsCollectMapper.allList(goodsVO.getId()));
        goodsVO.setIsNewName(GoodsIsNewEnum.getNameByCode(goodsVO.getIsNew()));
        goodsVO.setGoodsStatusName(GoodsStatusEnum.getNameByCode(goodsVO.getGoodsStatus()));

        // 卖家信息通过 Feign 获取
        User seller = fetchUserById(goodsVO.getSellerId());
        if (seller != null) {
            goodsVO.setSellerNickname(seller.getNickname());
            goodsVO.setSellerPic(seller.getUserPic());
        }
    }

    private User fetchUserById(Integer userId) {
        if (userId == null) return null;
        try {
            Result<User> result = userFeignClient.getById(userId);
            return result.getCode() == 0 ? result.getData() : null;
        } catch (Exception e) {
            // Feign 调用失败时降级，不阻塞主流程
            return null;
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<Goods> findAllOnSale() {
        return goodsMapper.findAllOnSale();
    }

    private Integer resolveGoodsStatus(Integer stock, Integer currentStatus) {
        if (stock != null && stock <= 0) {
            return GoodsStatusEnum.SOLD_OUT.getCode();
        }
        if (stock != null && stock > 0 && (currentStatus == null || Objects.equals(currentStatus, GoodsStatusEnum.SOLD_OUT.getCode()))) {
            return GoodsStatusEnum.ON_SALE.getCode();
        }
        return currentStatus;
    }
}
