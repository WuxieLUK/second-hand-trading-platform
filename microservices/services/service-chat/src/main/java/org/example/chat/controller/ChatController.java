package org.example.chat.controller;

import com.alibaba.csp.sentinel.annotation.SentinelResource;
import jakarta.validation.Valid;
import org.example.chat.DTO.ChatMsgSendDTO;
import org.example.chat.POJO.ChatMessage;
import org.example.chat.POJO.ChatSession;
import org.example.chat.exception.SentinelBlockHandler;
import org.example.chat.service.ChatDomainService;
import org.example.chat.util.ThreadLocalUtil;
import org.example.common.PageBean;
import org.example.common.Result;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
public class ChatController {

    private final ChatDomainService chatDomainService;

    public ChatController(ChatDomainService chatDomainService) {
        this.chatDomainService = chatDomainService;
    }

    @GetMapping("/myList")
    @SentinelResource(value = "chatMyList", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "chatListBlocked")
    public Result<List<ChatSession>> myChatList() {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Result.success(chatDomainService.getMyChatList((Integer) claims.get("id")));
    }

    @GetMapping("/msg/{sessionId}")
    public Result<PageBean<ChatMessage>> sessionMsg(@PathVariable Long sessionId,
                                                    @RequestParam(defaultValue = "1") Integer pageNum,
                                                    @RequestParam(defaultValue = "20") Integer pageSize) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Result.success(chatDomainService.getSessionMsg(sessionId, pageNum, pageSize, (Integer) claims.get("id")));
    }

    @PostMapping("/send")
    @SentinelResource(value = "chatSend", blockHandlerClass = SentinelBlockHandler.class, blockHandler = "chatSendBlocked")
    public Result<ChatMessage> sendMsg(@RequestBody @Valid ChatMsgSendDTO dto) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Result.success(chatDomainService.sendMsg(dto, (Integer) claims.get("id")));
    }

    @PutMapping("/markRead/{sessionId}")
    public Result<Void> markAsRead(@PathVariable Long sessionId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        chatDomainService.markMsgAsRead(sessionId, (Integer) claims.get("id"));
        return Result.success();
    }

    @PostMapping("/createSession/{receiverId}")
    public Result<ChatSession> createSession(@PathVariable Integer receiverId) {
        Map<String, Object> claims = ThreadLocalUtil.get();
        return Result.success(chatDomainService.createSession((Integer) claims.get("id"), receiverId));
    }
}
