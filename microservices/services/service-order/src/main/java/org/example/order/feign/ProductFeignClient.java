package org.example.order.feign;

import org.example.common.Result;
import org.example.goods.POJO.Goods;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;
import java.util.Map;

/**
 * service-product 的 Feign 客户端
 * 替代原有的 GoodsMapper 直接数据库访问
 */
@FeignClient(name = "service-product", fallbackFactory = ProductFeignClientFallbackFactory.class)
public interface ProductFeignClient {

    @GetMapping("/internal/goods/{id}")
    Result<GoodsDetailVO> getGoodsById(@PathVariable("id") Integer id);

    @PostMapping("/internal/goods/listByIds")
    Result<List<GoodsVO>> listByIds(@RequestBody List<Integer> ids);

    /**
     * 查询所有在售商品（用于库存缓存初始化）
     * 需要 service-product 暴露此端点
     */
    @GetMapping("/internal/goods/allOnSale")
    Result<List<Goods>> findAllOnSale();

    /**
     * 更新商品库存（下单扣减/取消恢复）
     */
    @PutMapping("/internal/goods/stock")
    Result<Void> updateStock(@RequestBody Map<String, Integer> params);
}
