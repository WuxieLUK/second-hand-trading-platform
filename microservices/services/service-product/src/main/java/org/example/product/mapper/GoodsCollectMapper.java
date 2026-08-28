package org.example.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface GoodsCollectMapper {

    @Select("select count(*) from goods_collect where goods_id = #{goodsId}")
    Integer allList(Integer goodsId);

    /**
     * 查询用户收藏的商品 ID 列表
     */
    @Select("select goods_id from goods_collect where user_id = #{userId} order by create_time desc")
    List<Integer> findCollectedGoodsIdsByUserId(Integer userId);

    /**
     * 协同过滤：根据用户已收藏商品，查找被"相同用户群体"高频收藏的其他商品
     * 即："收藏了这些商品的人也收藏了..."
     * 排除已收藏的商品
     */
    @Select({"<script>",
            "SELECT gc2.goods_id AS goodsId, COUNT(DISTINCT gc2.user_id) AS score",
            "FROM goods_collect gc1",
            "INNER JOIN goods_collect gc2 ON gc1.user_id = gc2.user_id",
            "WHERE gc1.goods_id IN",
            "<foreach collection='goodsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "AND gc2.goods_id NOT IN",
            "<foreach collection='goodsIds' item='id' open='(' separator=',' close=')'>#{id}</foreach>",
            "AND gc2.goods_id IS NOT NULL",
            "GROUP BY gc2.goods_id",
            "ORDER BY score DESC",
            "LIMIT #{limit}",
            "</script>"})
    List<java.util.Map<String, Object>> findCollaborativeItems(
            @Param("goodsIds") List<Integer> goodsIds,
            @Param("limit") int limit);
}
