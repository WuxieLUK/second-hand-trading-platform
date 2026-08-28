package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.pojo.GoodsCollect;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.GoodsCollectService;
import com.itheima.util.ThreadLocalUtil;
import com.itheima.vo.GoodsVO;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/goodsCollect")
@Validated
@Slf4j
public class GoodsCollectController {
    @Resource
    private GoodsCollectService goodsCollectService;

    /**
     * 添加商品收藏
     */
    @PreAuthorize("/goodsCollect/add/{goodsId}")
    @PostMapping("/add/{goodsId}")
    public Result add(@PathVariable Integer goodsId) {
        goodsCollectService.add(goodsId);
        return Result.success("收藏成功");
    }

    /**
     * 查询当前用户是否收藏该商品
     */
    @PreAuthorize("/goodsCollect/list/{goodsId}")
    @GetMapping("/list/{goodsId}")
    public Result<List<GoodsCollect>> list(@PathVariable Integer goodsId) {
        List<GoodsCollect> list = goodsCollectService.list(goodsId);
        return Result.success(list);
    }

    /**
     * 取消商品收藏
     */
    @PreAuthorize("/goodsCollect/delete/{goodsId}")
    @DeleteMapping("/delete/{goodsId}")
    public Result delete(@PathVariable Integer goodsId) {
        goodsCollectService.delete(goodsId);
        return Result.success("取消收藏成功");
    }

    /**
     * 我的商品收藏列表（分页）
     */
    @PreAuthorize("/goodsCollect/myList")
    @GetMapping("/myList")
    public Result<PageBean<GoodsVO>> myList(
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "10") Integer pageSize
    ) {
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        PageBean<GoodsVO> pageBean = goodsCollectService.myList(pageNum, pageSize, userId);
        return Result.success(pageBean);
    }

}