package com.itheima.mapper;

import com.itheima.pojo.GoodsCollect;
import com.itheima.vo.GoodsVO;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface GoodsCollectMapper {
    // 添加商品收藏
    @Insert("insert into goods_collect(user_id,goods_id,create_time) values(#{userId},#{goodsId},now())")
    void add(Integer userId, Integer goodsId);

    // 查询当前用户是否收藏该商品
    @Select("select * from goods_collect where user_id=#{userId} AND goods_id= #{goodsId}")
    List<GoodsCollect> list(Integer userId, Integer goodsId);

    // 取消商品收藏
    @Delete("delete from goods_collect where user_id= #{userId} AND goods_id= #{goodsId}")
    void delete(Integer userId, Integer goodsId);

    // 查询指定商品的所有收藏记录
    @Select("select COUNT(*) from goods_collect where goods_id= #{goodsId}")
    Integer allList(Integer goodsId);

    // 查询我的商品收藏列表（分页，XML实现）
    List<GoodsVO> myList(Integer userId);
}