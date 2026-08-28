package org.example.collect.service;

import org.example.common.PageBean;
import org.example.goods.POJO.GoodsCollect;
import org.example.goods.VO.GoodsVO;

import java.util.List;

public interface GoodsCollectDomainService {
    void add(Integer goodsId);

    List<GoodsCollect> list(Integer goodsId);

    void delete(Integer goodsId);

    PageBean<GoodsVO> myList(Integer pageNum, Integer pageSize, Integer userId);
}
