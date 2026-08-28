import request from "@/utils/request.js";

/**
 * 文章模块 API
 * ===========
 * 注：文章功能是此前规划但未完成的模块，后端尚无对应接口。
 * 以下函数保留为桩实现返回空数据，避免运行时 import 报错。
 * 后续如需启用文章功能，请替换为真实 API 路径。
 */

// 1. 获取我的文章收藏列表（分页）
export const articleMyCollectServices = (params) => {
    console.warn('[article.js] 文章功能尚未接入后端，返回空数据')
    return Promise.resolve({
        data: { items: [], total: 0 }
    })
}

// 2. 获取文章全部分类列表
export const articleCateListService = () => {
    console.warn('[article.js] 文章分类功能尚未接入后端，返回空数据')
    return Promise.resolve({
        data: []
    })
}

// 3. 获取文章列表（分页查询）
export const articleListService = (params) => {
    console.warn('[article.js] 文章列表功能尚未接入后端，返回空数据')
    return Promise.resolve({
        data: { items: [], total: 0 }
    })
}
