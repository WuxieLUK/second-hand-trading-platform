package org.example.collect.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.collect.feign.ProductFeignClient;
import org.example.collect.mapper.GoodsCollectMapper;
import org.example.collect.service.GoodsCollectDomainService;
import org.example.collect.util.ThreadLocalUtil;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.goods.POJO.GoodsCollect;
import org.example.goods.VO.GoodsDetailVO;
import org.example.goods.VO.GoodsVO;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class GoodsCollectDomainServiceImpl implements GoodsCollectDomainService {

    private final GoodsCollectMapper goodsCollectMapper;
    private final ProductFeignClient productFeignClient;

    public GoodsCollectDomainServiceImpl(GoodsCollectMapper goodsCollectMapper, ProductFeignClient productFeignClient) {
        this.goodsCollectMapper = goodsCollectMapper;
        this.productFeignClient = productFeignClient;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Integer goodsId) {
        Result<GoodsDetailVO> goodsResult = productFeignClient.getGoodsById(goodsId);
        if (goodsResult.getData() == null) {
            throw new IllegalArgumentException("收藏商品不存在");
        }
        Map<String, Object> claims = ThreadLocalUtil.get();
        Integer userId = (Integer) claims.get("id");
        if (goodsCollectMapper.list(userId, goodsId).isEmpty()) {
            goodsCollectMapper.add(userId, goodsId);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsCollect> list(Integer goodsId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return goodsCollectMapper.list((Integer) claims.get("id"), goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer goodsId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        goodsCollectMapper.delete((Integer) claims.get("id"), goodsId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<GoodsVO> myList(Integer pageNum, Integer pageSize, Integer userId) {
        PageHelper.startPage(pageNum, pageSize);
        List<GoodsCollect> collects = goodsCollectMapper.myList(userId);
        Page<GoodsCollect> page = (Page<GoodsCollect>) collects;
        List<Integer> goodsIds = collects.stream().map(GoodsCollect::getGoodsId).distinct().collect(Collectors.toList());
        List<GoodsVO> goods = new ArrayList<>();
        if (!goodsIds.isEmpty()) {
            Result<List<GoodsVO>> result = productFeignClient.listByIds(goodsIds);
            if (result.getData() != null) {
                goods = result.getData();
            }
        }
        PageBean<GoodsVO> pageBean = new PageBean<>();
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(goods);
        return pageBean;
    }
}
