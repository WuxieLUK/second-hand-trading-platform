package com.itheima.feign;

import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.GoodsDTO;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.VO.GoodsVO;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.example.user.VO.BuyerViewSellerVO;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

/**
 * service-product Feign 客户端（BFF 层调用）
 */
@FeignClient(name = "service-product")
public interface ProductFeignClient {

    @PostMapping("/goods/add")
    Result<Void> add(@RequestBody GoodsDTO goodsDTO);

    @PostMapping("/goods/list")
    Result<PageBean<GoodsVO>> list(@RequestBody GoodsQueryDTO queryDTO);

    @PostMapping("/goods/detail/{id}")
    Result<GoodsDetailVO> detail(@PathVariable("id") Integer id);

    @PostMapping("/goods/update")
    Result<Void> update(@RequestBody GoodsDTO goodsDTO);

    @PostMapping("/goods/delete/{id}")
    Result<Void> delete(@PathVariable("id") Integer id);

    @PostMapping("/goods/updateStatus/{id}/{status}")
    Result<Void> updateStatus(@PathVariable("id") Integer id, @PathVariable("status") Integer status);

    @PostMapping("/goods/goodsopenlist")
    Result<PageBean<GoodsVO>> goodsOpenList(@RequestBody GoodsQueryDTO queryDTO);

    @PostMapping("/goods/mylist")
    Result<PageBean<GoodsVO>> myList(@RequestBody GoodsQueryDTO queryDTO);

    @PostMapping("/goods/findSellerByUserId/{id}")
    Result<BuyerViewSellerVO> findSellerByUserId(@PathVariable("id") Integer id);

    @PostMapping("/goods/seller/alllist")
    Result<PageBean<GoodsVO>> sellerAllList(@RequestBody GoodsQueryDTO queryDTO);

    @GetMapping("/internal/goods/{id}")
    Result<GoodsDetailVO> internalDetail(@PathVariable("id") Integer id);

    @PostMapping("/internal/goods/listByIds")
    Result<List<GoodsVO>> listByIds(@RequestBody List<Integer> ids);

    @PostMapping("/internal/goods/search")
    Result<PageBean<GoodsVO>> search(@RequestBody GoodsQueryDTO queryDTO);

    @PostMapping("/internal/goods/ragSearch")
    Result<List<GoodsVO>> ragSearch(@RequestBody Map<String, String> params);

    @PostMapping("/internal/goods/ragSearchWithSummary")
    Result<Map<String, Object>> ragSearchWithSummary(@RequestBody Map<String, String> params);

    @PostMapping("/internal/goods/collaborativeRecommend")
    Result<List<GoodsVO>> collaborativeRecommend(@RequestBody Map<String, Object> params);

    @PostMapping("/internal/goods/recommendByCategory")
    Result<List<GoodsVO>> recommendByCategory(@RequestBody Map<String, Object> params);
}
