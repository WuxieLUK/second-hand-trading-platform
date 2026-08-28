import request from "@/utils/request.js";

/**
 * 创建订单
 * @param {Object} orderCreateDTO - 订单创建参数
 * @returns {Promise} 响应结果
 */
export const createOrderService = (orderCreateDTO) => {
    return request.post(`/api/order/order/create`, orderCreateDTO);
}

/**
 * 分页查询订单（多条件筛选）
 * @param {Object} params - 查询参数（OrderQueryDTO）
 * @returns {Promise} 分页结果
 */
export const getOrderListService = (params) => {
    return request.get(`/api/order/order/list`, { params });
}

/**
 * 查询订单详情
 * @param {Number} id - 订单ID
 * @returns {Promise} 订单详情
 */
export const getOrderDetailService = (id) => {
    return request.get(`/api/order/order/detail/${id}`);
}

/**
 * 支付订单
 * @param {Number} id - 订单ID
 * @param {Number} status - 订单状态（1待付款/2待发货/3待收货/4已完成/5已取消）
 * @returns {Promise} 响应结果
 */
export const updateOrderStatusService = (id) => {
    return request.put(`/api/order/order/status/${id}`);
}

/**
 * 取消订单
 * @param {Number} id - 订单ID
 * @returns {Promise} 响应结果
 */
export const cancelOrderService = (id) => {
    return request.put(`/api/order/order/cancel/${id}`);
}

/**
 * 订单发货
 * @param {Number} id - 订单ID
 * */
export const sendOrderService = (id) => {
    return request.put(`/api/order/order/send/${id}`);
}

/**
 * 确认收货
 * @param {Number} id - 订单ID
 * @returns {Promise} 响应结果
 */
export const confirmReceiveOrderService = (id) => {
    return request.put(`/api/order/order/confirm/${id}`);
}

/**
 * 申请退款
 * @param {Object} refundApplyDTO - 退款申请参数
 * @returns {Promise} 响应结果
 */
export const applyRefundService = (refundApplyDTO) => {
    return request.post(`/api/order/order/refund/apply`, refundApplyDTO);
}

/**
 * 处理退款（同意/驳回）
 * @param {Object} refundHandleDTO - 退款处理参数
 * @returns {Promise} 响应结果
 */
export const handleRefundService = (refundHandleDTO) => {
    return request.put(`/api/order/order/refund/handle`, refundHandleDTO);
}

/**
删除订单
*/
export const deleteOrderService = (id) => {
    return request.delete(`/api/order/order/delete/${id}`);
}


//根据订单编号查询某一条订单
export const findByOrderNoService = (orderNo) => {
    return request.get(`/api/order/order/findByOrderNo/${orderNo}`);
}

//管理员修改商品状态
export const adminUpdateStatusService = (id, status) => {
    return request.put(`/api/order/order/adminUpdateStatus/${id}/${status}`);
}
