package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import org.example.common.Result;
import com.itheima.service.DashboardService;
import org.example.common.DashboardStatsVO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/dashboard")
public class DashboardController {

    @Autowired
    private DashboardService dashboardService;

    /**
     * 获取仪表盘统计数据
     */
    @PreAuthorize("/dashboard/stats")
    @GetMapping("/stats")
    public Result<DashboardStatsVO> getDashboardStats() {
        DashboardStatsVO stats = dashboardService.getDashboardStats();
        return Result.success(stats);
    }
}