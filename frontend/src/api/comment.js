import request from "@/utils/request.js";

export const commentAddService = (commentData)=>{
    return request.post('/api/comment/comment/add',commentData)
}

//文章分页列表查询
export const commentListService = (params)=>{
    return request.get('/api/comment/comment/list',{params:params})
}

//文章修改
export const commentUpdateServices=(commentData)=>{
    return request.put(`/api/comment/comment/update`,commentData)
}

//文章删除
export const commentDeleteService=(id)=>{
    return request.delete(`/api/comment/comment/delete/${id}`)
}

//文章详情
export const commentList=(articleId)=>{
    return request.get(`/api/comment/comment/commentList/${articleId}`)
}


// 全量评论列表（改用已存在的 list 端点，默认返回第一页）
export const commentCountService=()=>{
    return request.get(`/api/comment/comment/list`)
}

export const commentLikeService=(id)=>{
    return request.post(`/api/comment/comment/like/${id}`)
}


//显示与某一个用户id有关的商品的全部评论
export const commentallListService=(userId)=>{
    return request.get(`/api/comment/comment/commentallList/${userId}`)
}

