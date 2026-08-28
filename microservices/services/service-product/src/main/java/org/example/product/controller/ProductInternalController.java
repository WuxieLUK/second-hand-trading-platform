package org.example.product.controller;

import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.POJO.Goods;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.example.product.service.ProductDomainService;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * 商品服务内部 API（供其他微服务通过 Feign 调用）
 * 这些端点不经过 LoginInterceptor 鉴权（由调用方保证认证）
 */
@RestController
@RequestMapping("/internal/goods")
public class ProductInternalController {

    private final ProductDomainService productDomainService;

    public ProductInternalController(ProductDomainService productDomainService) {
        this.productDomainService = productDomainService;
    }

    @GetMapping("/{id}")
    public Result<GoodsDetailVO> getById(@PathVariable Integer id) {
        return Result.success(productDomainService.findById(id));
    }

    @PostMapping("/listByIds")
    public Result<List<GoodsVO>> listByIds(@RequestBody List<Integer> ids) {
        return Result.success(productDomainService.listByIds(ids));
    }

    /**
     * 更新商品库存（供 service-order 下单/取消时调用）
     * @param params { "id": 1, "stockDelta": -1 }
     */
    @PutMapping("/stock")
    public Result<Void> updateStock(@RequestBody Map<String, Integer> params) {
        Integer id = params.get("id");
        Integer stockDelta = params.get("stockDelta");
        if (id == null || stockDelta == null) {
            return Result.error("参数不完整");
        }
        productDomainService.updateStock(id, stockDelta);
        return Result.success();
    }

    @GetMapping("/allOnSale")
    public Result<List<Goods>> findAllOnSale() {
        return Result.success(productDomainService.findAllOnSale());
    }

    /**
     * SQL 搜索商品（主搜索路径，零外部依赖）
     */
    @PostMapping("/search")
    public Result<PageBean<GoodsVO>> search(@RequestBody GoodsQueryDTO queryDTO) {
        return Result.success(productDomainService.search(queryDTO));
    }

    @PostMapping("/ragSearch")
    public Result<List<GoodsVO>> ragSearch(@RequestBody Map<String, String> params) {
        String query = params.get("query");
        if (query == null || query.isBlank()) {
            return Result.error("查询词不能为空");
        }
        return Result.success(productDomainService.ragSearch(query));
    }

    @PostMapping("/ragSearchWithSummary")
    public Result<java.util.Map<String, Object>> ragSearchWithSummary(@RequestBody Map<String, String> params) {
        String query = params.get("query");
        if (query == null || query.isBlank()) {
            return Result.error("查询词不能为空");
        }
        return Result.success(productDomainService.ragSearchWithSummary(query));
    }

    /**
     * 协同过滤推荐：根据用户收藏历史推荐商品
     */
    @PostMapping("/collaborativeRecommend")
    public Result<List<GoodsVO>> collaborativeRecommend(@RequestBody Map<String, Object> params) {
        Integer userId = params.get("userId") != null ? (Integer) params.get("userId") : null;
        int limit = params.get("limit") != null ? (Integer) params.get("limit") : 10;
        return Result.success(productDomainService.collaborativeRecommend(userId, limit));
    }

    /**
     * 按分类关键词推荐：匹配分类 → 该分类下热门商品 + 同分类协同过滤
     */
    @PostMapping("/recommendByCategory")
    public Result<List<GoodsVO>> recommendByCategory(@RequestBody Map<String, Object> params) {
        String keyword = (String) params.get("keyword");
        Integer userId = params.get("userId") != null ? (Integer) params.get("userId") : null;
        int limit = params.get("limit") != null ? (Integer) params.get("limit") : 10;
        return Result.success(productDomainService.recommendByCategory(keyword, userId, limit));
    }
}
