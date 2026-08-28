package com.itheima.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class DashboardStatsVO {
    private Integer userCount;          // 用户数量
    private Integer goodsCount;         // 闲置商品数量
    private List<CategoryStats> categoryStats; // 分类统计

    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    public static class CategoryStats {
        private String categoryName;     // 分类名称
        private Integer count;           // 商品数量
        private String color;            // 饼图颜色（可选）
    }
}