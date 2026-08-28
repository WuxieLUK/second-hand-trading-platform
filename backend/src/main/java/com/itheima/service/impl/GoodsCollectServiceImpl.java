package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.mapper.GoodsCollectMapper;
import com.itheima.pojo.Enum.GoodsIsNewEnum;
import com.itheima.pojo.Enum.GoodsStatusEnum;
import com.itheima.pojo.GoodsCollect;
import com.itheima.pojo.PageBean;
import com.itheima.service.GoodsCollectService;
import com.itheima.util.ThreadLocalUtil;
import com.itheima.vo.GoodsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Service
public class GoodsCollectServiceImpl implements GoodsCollectService {
    @Autowired
    private GoodsCollectMapper goodsCollectMapper;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void add(Integer goodsId) {
        // 从ThreadLocal获取当前登录用户ID
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        goodsCollectMapper.add(userId, goodsId);
    }

    @Override
    @Transactional(readOnly = true)
    public List<GoodsCollect> list(Integer goodsId) {
        // 查询当前用户是否收藏该商品
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        return goodsCollectMapper.list(userId, goodsId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void delete(Integer goodsId) {
        // 取消当前用户的商品收藏
        Map<String, Object> map = ThreadLocalUtil.get();
        Integer userId = (Integer) map.get("id");
        goodsCollectMapper.delete(userId, goodsId);
    }

    @Override
    @Transactional(readOnly = true)
    public PageBean<GoodsVO> myList(Integer pageNum, Integer pageSize, Integer userId) {
        // 分页查询我的商品收藏
        PageBean<GoodsVO> pb = new PageBean<>();
        PageHelper.startPage(pageNum, pageSize);
        List<GoodsVO> goodsList = goodsCollectMapper.myList(userId);
        Page<GoodsVO> p = (Page<GoodsVO>) goodsList;

        // 补充枚举名称（新旧程度/商品状态）
        goodsList.forEach(goods -> {
            goods.setIsNewName(GoodsIsNewEnum.getNameByCode(goods.getIsNew()));
            goods.setGoodsStatusName(GoodsStatusEnum.getNameByCode(goods.getGoodsStatus()));
        });

        pb.setTotal(p.getTotal());
        pb.setItems(p.getResult());
        return pb;
    }
}