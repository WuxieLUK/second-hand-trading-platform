package org.example.chat.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.chat.POJO.ChatMessage;
import org.example.chat.service.ChatPushGateway;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ChatPushGatewayImpl implements ChatPushGateway {

    @Override
    public void pushToUser(Integer userId, ChatMessage message) {
        log.info("push chat message to user {}, msgId={}", userId, message.getId());
    }
}
