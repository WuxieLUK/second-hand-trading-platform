package org.example.collect.feign;

import org.example.common.Result;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

import java.util.List;

@FeignClient(name = "service-product")
public interface ProductFeignClient {

    @GetMapping("/goods/internal/{id}")
    Result<GoodsDetailVO> getGoodsById(@PathVariable("id") Integer id);

    @PostMapping("/goods/internal/listByIds")
    Result<List<GoodsVO>> listByIds(@RequestBody List<Integer> ids);
}
