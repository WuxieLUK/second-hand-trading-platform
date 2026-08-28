package com.itheima.mapper;

import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

public interface DashboardMapper {

    @Select("SELECT COUNT(*) FROM user")
    Integer countTotalUsers();

    @Select("SELECT COUNT(*) FROM goods") // 1=在售
    Integer countOnSaleGoods();

    @Select("SELECT c.category_name AS categoryName, COUNT(g.id) AS count " +
            "FROM shop_category c " +
            "LEFT JOIN goods g ON c.id = g.category_id " +
            "GROUP BY c.id, c.category_name")
    List<Map<String, Object>> countGoodsByCategory();
}