package org.example.collect.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.example.collect.exception.SentinelBlockHandler;
import org.example.collect.service.GoodsCollectDomainService;
import org.example.collect.util.ThreadLocalUtil;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.POJO.GoodsCollect;
import org.example.goods.VO.GoodsVO;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goodsCollect")
public class GoodsCollectController {

    private final GoodsCollectDomainService goodsCollectDomainService;

    public GoodsCollectController(GoodsCollectDomainService goodsCollectDomainService) {
        this.goodsCollectDomainService = goodsCollectDomainService;
    }

    @PostMapping("/add/{goodsId}")
    @SentinelResource(value = "collectAdd", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "collectAddBlocked")
    public Result<Void> add(@PathVariable Integer goodsId) {
        goodsCollectDomainService.add(goodsId);
        return Result.success();
    }

    @GetMapping("/list/{goodsId}")
    public Result<List<GoodsCollect>> list(@PathVariable Integer goodsId) {
        return Result.success(goodsCollectDomainService.list(goodsId));
    }

    @DeleteMapping("/delete/{goodsId}")
    public Result<Void> delete(@PathVariable Integer goodsId) {
        goodsCollectDomainService.delete(goodsId);
        return Result.success();
    }

    @GetMapping("/myList")
    @SentinelResource(value = "collectMyList", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "collectListBlocked")
    public Result<PageBean<GoodsVO>> myList(@RequestParam(defaultValue = "1") Integer pageNum,
                                            @RequestParam(defaultValue = "10") Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Result.success(goodsCollectDomainService.myList(pageNum, pageSize, (Integer) claims.get("id")));
    }
}
