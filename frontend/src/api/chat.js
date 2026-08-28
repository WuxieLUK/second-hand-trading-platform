import request from "@/utils/request.js";

/**
 * 获取我的聊天列表（所有会话/聊天对象）
 * @returns {Promise} 会话列表数据
 */
export const getMyChatListService = () => {
    return request.get('/api/chat/chat/myList');
}

/**
 * 获取指定会话的历史消息（分页）
 * @param {Number} sessionId 会话ID
 * @param {Number} pageNum 页码（默认1）
 * @param {Number} pageSize 页大小（默认20）
 * @returns {Promise} 分页消息列表
 */
export const getSessionMsgService = (sessionId, pageNum = 1, pageSize = 20) => {
    return request.get(`/api/chat/chat/msg/${sessionId}`, {
        params: {
            pageNum,
            pageSize
        }
    });
}

/**
 * 发送聊天消息
 * @param {Object} msgData 消息数据
 * @param {Number} msgData.sessionId 会话ID（可选：无会话时传0或null）
 * @param {Number} msgData.receiverId 接收者ID（必填）
 * @param {String} msgData.content 消息内容（必填）
 * @param {Number} msgData.msgType 消息类型（默认1-文本）
 * @returns {Promise} 发送成功的消息对象
 */
export const sendChatMsgService = (msgData) => {
    return request.post('/api/chat/chat/send', msgData);
}

/**
 * 标记会话消息为已读
 * @param {Number} sessionId 会话ID
 * @returns {Promise} 操作结果
 */
export const markChatMsgAsReadService = (sessionId) => {
    return request.put(`/api/chat/chat/markRead/${sessionId}`);
}

/**
 * （可选扩展）创建会话（前端一般无需主动调用，发送消息时后端自动创建）
 * @param {Number} receiverId 接收者ID
 * @returns {Promise} 会话对象
 */
export const createChatSessionService = (receiverId) => {
    return request.post(`/api/chat/chat/createSession/${receiverId}`);
}