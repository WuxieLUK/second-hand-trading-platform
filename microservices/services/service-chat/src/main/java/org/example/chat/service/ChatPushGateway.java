package org.example.chat.service;

import org.example.chat.POJO.ChatMessage;

public interface ChatPushGateway {
    void pushToUser(Integer userId, ChatMessage message);
}
