package org.example.chat.exception;

import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.example.chat.DTO.ChatMsgSendDTO;
import org.example.chat.POJO.ChatMessage;
import org.example.chat.POJO.ChatSession;
import org.example.common.Result;

import java.util.List;

public final class SentinelBlockHandler {

    private SentinelBlockHandler() {
    }

    public static Result<List<ChatSession>> chatListBlocked(BlockException exception) {
        return Result.error("聊天列表已触发限流: " + exception.getClass().getSimpleName());
    }

    public static Result<ChatMessage> chatSendBlocked(ChatMsgSendDTO dto, BlockException exception) {
        return Result.error("聊天发送已触发限流: " + exception.getClass().getSimpleName());
    }
}
