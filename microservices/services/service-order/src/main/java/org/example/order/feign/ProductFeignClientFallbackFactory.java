package org.example.order.feign;

import lombok.extern.slf4j.Slf4j;
import org.example.common.Result;
import org.example.goods.POJO.Goods;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.springframework.cloud.openfeign.FallbackFactory;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.List;
import java.util.Map;

/**
 * ProductFeignClient 的 Fallback 工厂
 * 当 service-product 不可用、超时或触发 Sentinel 熔断时，
 * 返回明确的错误 Result，避免直接抛 500
 */
@Slf4j
@Component
public class ProductFeignClientFallbackFactory implements FallbackFactory<ProductFeignClient> {

    private static final String FALLBACK_MSG = "商品服务暂不可用，请稍后重试";

    @Override
    public ProductFeignClient create(Throwable cause) {
        log.error("ProductFeignClient fallback, cause: ", cause);

        return new ProductFeignClient() {
            @Override
            public Result<GoodsDetailVO> getGoodsById(Integer id) {
                log.warn("getGoodsById fallback, goodsId={}", id);
                return Result.error(FALLBACK_MSG);
            }

            @Override
            public Result<List<GoodsVO>> listByIds(List<Integer> ids) {
                log.warn("listByIds fallback, ids={}", ids);
                return Result.error(FALLBACK_MSG);
            }

            @Override
            public Result<List<Goods>> findAllOnSale() {
                log.warn("findAllOnSale fallback");
                return Result.error(FALLBACK_MSG);
            }

            @Override
            public Result<Void> updateStock(Map<String, Integer> params) {
                log.warn("updateStock fallback, params={}", params);
                return Result.error(FALLBACK_MSG);
            }
        };
    }
}
