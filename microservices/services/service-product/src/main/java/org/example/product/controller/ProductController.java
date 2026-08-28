package org.example.product.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import jakarta.validation.Valid;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.DTO.GoodsDTO;
import org.example.goods.DTO.GoodsQueryDTO;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.example.product.exception.SentinelBlockHandler;
import org.example.product.service.ProductDomainService;
import org.example.user.VO.BuyerViewSellerVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/goods")
public class ProductController {

    private final ProductDomainService productDomainService;

    public ProductController(ProductDomainService productDomainService) {
        this.productDomainService = productDomainService;
    }

    @PostMapping("/add")
    @SentinelResource(value = "productAdd", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "addBlocked")
    public Result<Void> add(@RequestBody @Valid GoodsDTO goodsDTO) {
        productDomainService.add(goodsDTO);
        return Result.success();
    }

    @PostMapping("/list")
    @SentinelResource(value = "productList", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "goodsListBlocked")
    public Result<PageBean<GoodsVO>> list(@RequestBody GoodsQueryDTO queryDTO) {
        return Result.success(productDomainService.alllist(queryDTO));
    }

    @PostMapping("/detail/{id}")
    public Result<GoodsDetailVO> detail(@PathVariable Integer id) {
        return Result.success(productDomainService.findById(id));
    }

    @PostMapping("/update")
    public Result<Void> update(@RequestBody @Valid GoodsDTO goodsDTO) {
        productDomainService.update(goodsDTO);
        return Result.success();
    }

    @PostMapping("/delete/{id}")
    public Result<Void> delete(@PathVariable Integer id) {
        productDomainService.delete(id);
        return Result.success();
    }

    @PostMapping("/updateStatus/{id}/{status}")
    public Result<Void> updateStatus(@PathVariable Integer id, @PathVariable Integer status) {
        productDomainService.updateStatus(id, status);
        return Result.success();
    }

    @PostMapping("/goodsopenlist")
    public Result<PageBean<GoodsVO>> goodsOpenList(@RequestBody GoodsQueryDTO queryDTO) {
        return Result.success(productDomainService.list(queryDTO));
    }

    @PostMapping("/mylist")
    public Result<PageBean<GoodsVO>> myList(@RequestBody GoodsQueryDTO queryDTO) {
        return Result.success(productDomainService.alllist(queryDTO));
    }

    @PostMapping("/findSellerByUserId/{id}")
    public Result<BuyerViewSellerVO> findSellerByUserId(@PathVariable Integer id) {
        return Result.success(productDomainService.findSellerByUserId(id));
    }

    @PostMapping("/seller/alllist")
    public Result<PageBean<GoodsVO>> sellerAllList(@RequestBody GoodsQueryDTO queryDTO) {
        return Result.success(productDomainService.alllist(queryDTO));
    }

    @GetMapping("/internal/{id}")
    public Result<GoodsDetailVO> internalDetail(@PathVariable Integer id) {
        return Result.success(productDomainService.findById(id));
    }

    @PostMapping("/internal/listByIds")
    public Result<List<GoodsVO>> listByIds(@RequestBody List<Integer> ids) {
        return Result.success(productDomainService.listByIds(ids));
    }

}
