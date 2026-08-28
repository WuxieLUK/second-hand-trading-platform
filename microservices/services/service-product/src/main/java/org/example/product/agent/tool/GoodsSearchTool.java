package org.example.product.agent.tool;

import dev.langchain4j.agent.tool.Tool;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.goods.POJO.Category;
import org.example.goods.POJO.Goods;
import org.example.product.infrastructure.ai.QwenEmbeddingUtil;
import org.example.product.infrastructure.milvus.MilvusVectorUtil;
import org.example.product.mapper.GoodsMapper;
import org.example.product.mapper.ShopCategoryMapper;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

/**
 * 商品搜索工具 —— 供 LangChain4j Agent 调用
 * <p>
 * 从 service-main 迁移而来，使用 service-product 本地 Mapper 查询商品数据。
 */
@Slf4j
@Component
public class GoodsSearchTool {

    @Resource
    private GoodsMapper goodsMapper;

    @Resource
    private ShopCategoryMapper shopCategoryMapper;

    @Resource
    private QwenEmbeddingUtil qwenEmbeddingUtil;

    @Resource
    private MilvusVectorUtil milvusVectorUtil;

    /**
     * Agent 工具：搜索在售商品
     */
    @Tool("Search on-sale campus second-hand goods by keyword, category, or price range. Returns formatted goods list with name, price, condition, and stock.")
    public String searchGoods(String keyword, Integer categoryId, BigDecimal maxPrice, Integer limit) {
        log.info("Agent 调用 GoodsSearchTool: keyword={}, categoryId={}, maxPrice={}, limit={}",
                keyword, categoryId, maxPrice, limit);

        if (limit == null || limit < 1) limit = 10;
        if (limit > 50) limit = 50;

        try {
            List<Goods> allOnSale = goodsMapper.listOnSaleGoods();

            List<Goods> filtered = allOnSale.stream()
                    .filter(g -> g.getGoodsStatus() != null && g.getGoodsStatus() == 1)
                    .filter(g -> keyword == null || keyword.isBlank()
                            || g.getGoodsName().toLowerCase().contains(keyword.toLowerCase())
                            || (g.getGoodsDesc() != null
                            && g.getGoodsDesc().toLowerCase().contains(keyword.toLowerCase())))
                    .filter(g -> categoryId == null || categoryId <= 0
                            || (g.getCategoryId() != null && g.getCategoryId().equals(categoryId)))
                    .filter(g -> maxPrice == null
                            || (g.getSellPrice() != null && g.getSellPrice().compareTo(maxPrice) <= 0))
                    .limit(limit)
                    .toList();

            // 关键词搜索无结果 → RAG 向量检索回退（语义相似匹配）
            if (filtered.isEmpty() && keyword != null && !keyword.isBlank()) {
                try {
                    log.info("关键词无匹配，尝试 RAG 向量检索回退: keyword={}", keyword);
                    List<Double> queryVector = qwenEmbeddingUtil.getEmbedding(keyword);
                    List<Long> goodsIds = milvusVectorUtil.searchSimilarGoods(queryVector, limit);
                    for (Long gid : goodsIds) {
                        Goods g = goodsMapper.findById(gid.intValue());
                        if (g != null && g.getGoodsStatus() != null && g.getGoodsStatus() == 1) {
                            // 再应用一次类目/价格过滤
                            if (categoryId != null && categoryId > 0
                                    && !categoryId.equals(g.getCategoryId())) continue;
                            if (maxPrice != null && g.getSellPrice() != null
                                    && g.getSellPrice().compareTo(maxPrice) > 0) continue;
                            filtered.add(g);
                        }
                    }
                    log.info("RAG 向量检索回退结果: {} 件", filtered.size());
                } catch (Exception e) {
                    log.warn("RAG 向量检索回退失败, keyword={}", keyword, e);
                }
            }

            if (filtered.isEmpty()) {
                return "抱歉，当前没有找到符合条件的在售商品。您可以试试其他关键词或分类。";
            }

            List<Category> categories = shopCategoryMapper.list();
            Map<Integer, String> categoryMap = categories.stream()
                    .collect(Collectors.toMap(Category::getId, Category::getCategoryName,
                            (a, b) -> a));

            StringBuilder sb = new StringBuilder();
            sb.append(String.format("共找到 %d 件在售商品：\n\n", filtered.size()));

            // 同时输出 PRODUCT_CARD 块（供前端渲染商品卡片）和文本列表
            for (int i = 0; i < filtered.size(); i++) {
                Goods g = filtered.get(i);
                String newDegree = formatNewDegree(g.getIsNew());
                String catName = categoryMap.getOrDefault(g.getCategoryId(), "未分类");
                String firstImage = g.getGoodsPic() != null
                        ? g.getGoodsPic().split(",")[0].trim() : "";

                // 商品卡片块（前端会解析渲染为可点击卡片）
                // 注意：JSON 必须放在单独一行且不可包含换行，否则 qwen3 模型可能损坏 JSON 结构
                String cardJson = String.format(
                        "{\"id\":%d,\"name\":\"%s\",\"price\":%.2f,\"image\":\"%s\",\"category\":\"%s\",\"degree\":\"%s\",\"stock\":%d,\"originalPrice\":%.2f}",
                        g.getId(), g.getGoodsName(),
                        g.getSellPrice(), firstImage, catName, newDegree,
                        g.getStock() != null ? g.getStock() : 0,
                        g.getOriginalPrice() != null ? g.getOriginalPrice() : BigDecimal.ZERO);
                sb.append("[PRODUCT_CARD]").append(cardJson).append("[/PRODUCT_CARD]\n\n");

                // 文本列表（简洁版，核心信息在卡片中展示）
                sb.append(String.format(
                        "%d. **%s** — ¥%.2f（%s，库存%d）\n",
                        i + 1, g.getGoodsName(),
                        g.getSellPrice(),
                        newDegree,
                        g.getStock() != null ? g.getStock() : 0
                ));
            }

            sb.append("提示：使用商品ID可查看更详细的信息或下单购买。");
            return sb.toString();

        } catch (Exception e) {
            log.error("商品搜索失败", e);
            return "商品搜索时遇到系统异常，请稍后重试。";
        }
    }

    /**
     * Agent 工具：查询单个商品详情
     */
    @Tool("Get detailed information about a specific goods by its ID, including description, price, condition, and image info.")
    public String getGoodsDetail(Integer goodsId) {
        log.info("Agent 调用 GoodsSearchTool.getGoodsDetail: goodsId={}", goodsId);

        if (goodsId == null || goodsId <= 0) {
            return "商品ID无效，请提供正确的商品编号。";
        }

        try {
            Goods goods = goodsMapper.findById(goodsId);
            if (goods == null) {
                return String.format("商品ID为 %d 的商品不存在。", goodsId);
            }

            String newDegree = formatNewDegree(goods.getIsNew());
            String statusName = formatGoodsStatus(goods.getGoodsStatus());

            return String.format(
                    "**%s** 的商品详情：\n\n" +
                            "- **商品ID**：%d\n" +
                            "- **描述**：%s\n" +
                            "- **分类ID**：%d\n" +
                            "- **售价**：¥%.2f\n" +
                            "- **原价**：¥%.2f\n" +
                            "- **新旧程度**：%s\n" +
                            "- **库存**：%d件\n" +
                            "- **状态**：%s\n" +
                            "- **上架时间**：%s\n",
                    goods.getGoodsName(),
                    goods.getId(),
                    goods.getGoodsDesc() != null ? goods.getGoodsDesc() : "暂无描述",
                    goods.getCategoryId(),
                    goods.getSellPrice(),
                    goods.getOriginalPrice() != null ? goods.getOriginalPrice() : BigDecimal.ZERO,
                    newDegree,
                    goods.getStock() != null ? goods.getStock() : 0,
                    statusName,
                    goods.getCreateTime() != null ? goods.getCreateTime().toString() : "未知"
            );

        } catch (Exception e) {
            log.error("查询商品详情失败, goodsId={}", goodsId, e);
            return "查询商品详情时遇到系统异常，请稍后重试。";
        }
    }

    private String formatNewDegree(Integer isNew) {
        if (isNew == null) return "二手";
        return switch (isNew) {
            case 0 -> "二手";
            case 1 -> "全新";
            case 2 -> "9成新";
            case 3 -> "8成新";
            case 4 -> "7成及以下";
            default -> "二手";
        };
    }

    private String formatGoodsStatus(Integer status) {
        if (status == null) return "未知";
        return switch (status) {
            case 1 -> "在售";
            case 2 -> "已售出";
            case 3 -> "下架";
            case 4 -> "审核中";
            case 5 -> "违规封禁";
            default -> "未知";
        };
    }
}
