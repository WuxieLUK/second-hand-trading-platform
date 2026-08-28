package com.itheima.service;

import com.itheima.DTO.GoodsDTO;
import com.itheima.DTO.GoodsQueryDTO;
import com.itheima.pojo.Goods;
import com.itheima.pojo.PageBean;
import com.itheima.vo.BuyerViewSellerVO;
import com.itheima.vo.GoodsDetailVO;
import com.itheima.vo.GoodsVO;

import java.util.List;

/**
 * 商品服务接口
 */
public interface GoodsService {
    void add(GoodsDTO goodsDTO);

    PageBean<GoodsVO> list(GoodsQueryDTO queryDTO);

    GoodsDetailVO findById(Integer id);

    void update(GoodsDTO goodsDTO);

    void delete(Integer id);

    void updateStatus(Integer id, Integer status);

    BuyerViewSellerVO findSellerByUserId(Integer id);

    List<GoodsVO> ragSearch(String query);

    /**
     * 全量校验并清理Milvus脏数据
     */
    void cleanMilvusDirtyData();

    /**
     * 全量查询商品
     * @param queryDTO
     * @return
     */
    PageBean<GoodsVO> alllist(GoodsQueryDTO queryDTO);
}