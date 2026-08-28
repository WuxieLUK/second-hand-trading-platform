package com.itheima.service;

import com.itheima.DTO.ChatMsgSendDTO;
import com.itheima.pojo.ChatMessage;
import com.itheima.pojo.ChatSession;
import com.itheima.pojo.PageBean;

import java.util.List;

/**
 * 聊天服务接口
 */
public interface ChatService {
    /**
     * 获取我的聊天列表（所有会话）
     */
    List<ChatSession> getMyChatList(Integer userId);

    /**
     * 获取会话历史消息（分页）
     */
    PageBean<ChatMessage> getSessionMsg(Long sessionId, Integer pageNum, Integer pageSize, Integer userId);

    /**
     * 发送消息（自动创建会话+更新会话+推送消息）
     */
    ChatMessage sendMsg(ChatMsgSendDTO dto, Integer senderId);

    /**
     * 标记会话消息为已读
     */
    void markMsgAsRead(Long sessionId, Integer userId);


    /**
     * 创建会话
     */
    ChatSession createSession(Integer senderId, Integer receiverId);
}