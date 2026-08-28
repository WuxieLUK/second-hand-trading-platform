package org.example.collect.mapper;

import org.apache.ibatis.annotations.*;
import org.example.goods.POJO.GoodsCollect;

import java.util.List;

@Mapper
public interface GoodsCollectMapper {

    @Insert("insert into goods_collect(user_id, goods_id, create_time) values(#{userId}, #{goodsId}, now())")
    void add(@Param("userId") Integer userId, @Param("goodsId") Integer goodsId);

    @Select("select * from goods_collect where user_id = #{userId} and goods_id = #{goodsId}")
    List<GoodsCollect> list(@Param("userId") Integer userId, @Param("goodsId") Integer goodsId);

    @Delete("delete from goods_collect where user_id = #{userId} and goods_id = #{goodsId}")
    void delete(@Param("userId") Integer userId, @Param("goodsId") Integer goodsId);

    @Select("select * from goods_collect where user_id = #{userId} order by create_time desc")
    List<GoodsCollect> myList(Integer userId);
}
