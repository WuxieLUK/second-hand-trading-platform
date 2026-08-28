package org.example.comment.feign;

import org.example.common.Result;
import org.example.goods.VO.GoodsDetailVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "service-product")
public interface ProductFeignClient {

    @GetMapping("/goods/internal/{id}")
    Result<GoodsDetailVO> getGoodsById(@PathVariable("id") Integer id);
}
