package com.itheima.service.impl;

import com.itheima.mapper.DashboardMapper;
import com.itheima.service.DashboardService;
import com.itheima.vo.DashboardStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

@Service
public class DashboardServiceImpl implements DashboardService {

    @Autowired
    private DashboardMapper dashboardMapper;

    // 分类颜色映射（与饼图图例对应）
    private static final Map<String, String> CATEGORY_COLORS = new HashMap<>();
    static {
        CATEGORY_COLORS.put("手机 / 数码 / 电脑", "#4066b2");
        CATEGORY_COLORS.put("服饰 / 箱包 / 运动", "#88b864");
        CATEGORY_COLORS.put("技能 / 卡券 / 潮玩", "#e6b84c");
        CATEGORY_COLORS.put("母婴 / 美妆 / 个护", "#d65454");
        CATEGORY_COLORS.put("家具 / 家电 / 家装", "#64a8b8");
        CATEGORY_COLORS.put("文玩 / 珠宝 / 礼品", "#447858");
        CATEGORY_COLORS.put("食品 / 宠物 / 花卉", "#e6783c");
        CATEGORY_COLORS.put("图书 / 游戏 / 音像", "#784488");
        CATEGORY_COLORS.put("汽车 / 电动车 / 租房", "#d664a8");
        CATEGORY_COLORS.put("五金 / 设备 / 农牧", "#4056b2");
    }

    @Override
    public DashboardStatsVO getDashboardStats() {
        DashboardStatsVO vo = new DashboardStatsVO();

        // 1. 查询用户总数
        vo.setUserCount(dashboardMapper.countTotalUsers());

        // 2. 查询闲置商品总数（状态=在售）
        vo.setGoodsCount(dashboardMapper.countOnSaleGoods());

        // 3. 查询各分类商品数量
        List<Map<String, Object>> categoryData = dashboardMapper.countGoodsByCategory();
        List<DashboardStatsVO.CategoryStats> categoryStats = new ArrayList<>();

        for (Map<String, Object> data : categoryData) {
            String categoryName = (String) data.get("categoryName");
            Integer count = ((Number) data.get("count")).intValue();
            String color = CATEGORY_COLORS.getOrDefault(categoryName, "#999999");
            categoryStats.add(new DashboardStatsVO.CategoryStats(categoryName, count, color));
        }
        vo.setCategoryStats(categoryStats);

        return vo;
    }
}