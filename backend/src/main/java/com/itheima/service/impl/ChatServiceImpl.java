package com.itheima.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import com.itheima.DTO.ChatMsgSendDTO;
import com.itheima.mapper.ChatMessageMapper;
import com.itheima.mapper.ChatSessionMapper;
import com.itheima.pojo.ChatMessage;
import com.itheima.pojo.ChatSession;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.User;
import com.itheima.service.ChatService;
import com.itheima.service.UserService;
import com.itheima.websocket.WebSocketServer;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class ChatServiceImpl implements ChatService {
    @Autowired
    private ChatSessionMapper sessionMapper;
    @Autowired
    private ChatMessageMapper messageMapper;
    @Autowired
    private WebSocketServer webSocketServer;

    @Autowired
    private UserService userService;

    /**
     * 获取我的聊天列表（核心：补充当前用户的未读数）
     */
    @Override
    @Transactional(readOnly = true)
    public List<ChatSession> getMyChatList(Integer userId) {
        List<ChatSession> sessionList = sessionMapper.selectMyChatList(userId);
        // 补充当前用户的未读数
        sessionList.forEach(session -> {
            if (session.getFromUserId().equals(userId)) {
                session.setUnreadCount(session.getFromUnread());
            } else {
                session.setUnreadCount(session.getToUnread());
            }
        });
        return sessionList;
    }

    /**
     * 获取会话历史消息（分页+标记已读）
     */
    @Override
    public PageBean<ChatMessage> getSessionMsg(Long sessionId, Integer pageNum, Integer pageSize, Integer userId) {
        PageBean<ChatMessage> pb = new PageBean<>();
        // 先标记该会话消息为已读
        messageMapper.markMsgAsRead(sessionId, userId);
        sessionMapper.clearUnread(sessionId, userId);

        // 分页查询消息
        PageHelper.startPage(pageNum, pageSize);
        int offset = (pageNum - 1) * pageSize;
        List<ChatMessage> msgList = messageMapper.selectMsgBySession(sessionId);
        Page<ChatMessage> page = (Page<ChatMessage>) msgList;

        pb.setTotal(page.getTotal());
        pb.setItems(page.getResult());
        return pb;
    }

    /**
     * 发送消息：核心逻辑（自动创建会话+插入消息+更新会话+WebSocket推送）
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendMsg(ChatMsgSendDTO dto, Integer senderId) {
        Integer receiverId = dto.getReceiverId();
        String content = dto.getContent();

        // 1. 查询/创建会话（适配user_pair_min/max唯一索引）
        ChatSession session = sessionMapper.selectSessionByTwoUser(senderId, receiverId);
        if (session == null) {
            // 创建新会话（计算列无需手动赋值）
            session = new ChatSession();
            session.setFromUserId(senderId);
            session.setToUserId(receiverId);
            session.setLastMsg(content);
            sessionMapper.insertSession(session);
        }

        // 2. 插入消息
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setContent(content);
        message.setMsgType(dto.getMsgType());
        messageMapper.insertMessage(message);

        // 3. 更新会话最后一条消息+未读数
        sessionMapper.updateSessionLastMsg(session.getId(), content, receiverId);

        //通过id获取用户信息
        User user = userService.getById(senderId);
        message.setSenderAvatar(user.getUserPic());
        message.setSenderNickname(user.getNickname());

        // 4. WebSocket推送消息给接收者
        webSocketServer.sendToUser(receiverId.toString(), message);

        return message;
    }

    /**
     * 标记消息为已读
     */
    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMsgAsRead(Long sessionId, Integer userId) {
        messageMapper.markMsgAsRead(sessionId, userId);
        sessionMapper.clearUnread(sessionId, userId);
    }

    @Override
    public ChatSession createSession(Integer senderId, Integer receiverId) {
        ChatSession session = sessionMapper.selectSessionByTwoUser(senderId, receiverId);
        if (session == null) {
            session = new ChatSession();
            session.setFromUserId(senderId);
            session.setToUserId(receiverId);
            session.setLastMsg(""); // 初始无消息
            session.setStatus(1); // 正常状态
            sessionMapper.insertSession(session);
        }
        return session;
    }
}