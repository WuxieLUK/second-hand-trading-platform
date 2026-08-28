package com.itheima.service;

import com.itheima.pojo.GoodsCollect;
import com.itheima.pojo.PageBean;
import com.itheima.vo.GoodsVO;

import java.util.List;

/**
 * 商品收藏服务接口
 */
public interface GoodsCollectService {
    // 添加商品收藏
    void add(Integer goodsId);

    // 查询当前用户是否收藏该商品
    List<GoodsCollect> list(Integer goodsId);

    // 取消商品收藏
    void delete(Integer goodsId);

    // 我的商品收藏列表（分页）
    PageBean<GoodsVO> myList(Integer pageNum, Integer pageSize, Integer userId);
}