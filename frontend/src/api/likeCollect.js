import request from "@/utils/request.js";

// ====================== 商品收藏相关 API ======================
// 商品收藏添加
export const goodsAddCollect = (goodsId) => {
    return request.post(`/api/goodsCollect/goodsCollect/add/${goodsId}`)
}

// 商品收藏删除（取消收藏）
export const goodsDeleteCollect = (goodsId) => {
    return request.delete(`/api/goodsCollect/goodsCollect/delete/${goodsId}`)
}

// 查询当前用户是否收藏该商品
export const goodsListByCollectId = (goodsId) => {
    return request.get(`/api/goodsCollect/goodsCollect/list/${goodsId}`)
}

// 我的商品收藏列表（分页）
export const goodsMyCollectList = (pageNum = 1, pageSize = 10) => {
    return request.get(`/api/goodsCollect/goodsCollect/myList`, {
        params: {
            pageNum,
            pageSize
        }
    })
}