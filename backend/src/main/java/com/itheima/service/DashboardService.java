package com.itheima.service;

import com.itheima.vo.DashboardStatsVO;

/**
 * 数据仪表盘服务接口
 * 负责提供运营数据统计、可视化所需的核心数据
 */
public interface DashboardService {

    /**
     * 获取仪表盘核心统计数据
     * 包含用户总数、在售商品总数、商品分类统计等
     * @return 仪表盘统计VO对象
     */
    DashboardStatsVO getDashboardStats();
}