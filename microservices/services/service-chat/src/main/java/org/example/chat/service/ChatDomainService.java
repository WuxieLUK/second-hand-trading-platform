package org.example.chat.service;

import org.example.chat.DTO.ChatMsgSendDTO;
import org.example.chat.POJO.ChatMessage;
import org.example.chat.POJO.ChatSession;
import org.example.common.PageBean;

import java.util.List;

public interface ChatDomainService {
    List<ChatSession> getMyChatList(Integer userId);

    PageBean<ChatMessage> getSessionMsg(Long sessionId, Integer pageNum, Integer pageSize, Integer userId);

    ChatMessage sendMsg(ChatMsgSendDTO dto, Integer senderId);

    void markMsgAsRead(Long sessionId, Integer userId);

    ChatSession createSession(Integer senderId, Integer receiverId);
}
