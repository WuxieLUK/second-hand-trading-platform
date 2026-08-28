import request from "@/utils/request.js";

// ====================== 数据仪表盘相关接口 ======================
/**
 * 获取仪表盘统计数据（用户数、商品数、分类统计）
 * @returns {Promise} 统计数据Promise
 */
export const getDashboardStatsService = () => {
    return request.get('/api/backAll/dashboard/stats');
};
