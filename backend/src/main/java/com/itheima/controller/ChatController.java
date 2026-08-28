package com.itheima.controller;

import com.itheima.anno.PreAuthorize;
import com.itheima.DTO.ChatMsgSendDTO;
import com.itheima.pojo.ChatMessage;
import com.itheima.pojo.ChatSession;
import com.itheima.pojo.PageBean;
import com.itheima.pojo.Result;
import com.itheima.service.ChatService;
import com.itheima.util.ThreadLocalUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/chat")
@Validated
@Slf4j
public class ChatController {
    @Resource
    private ChatService chatService;

    /**
     * 核心接口：获取我的聊天列表（所有会话/聊天对象）
     */
    @PreAuthorize("/chat/myList")
    @GetMapping("/myList")
    public Result<List<ChatSession>> myChatList() {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer userId = (Integer) userMap.get("id");
        List<ChatSession> list = chatService.getMyChatList(userId);
        return Result.success(list);
    }

    /**
     * 获取会话历史消息（分页）
     */
    @PreAuthorize("/chat/msg/{sessionId}")
    @GetMapping("/msg/{sessionId}")
    public Result<PageBean<ChatMessage>> sessionMsg(
            @PathVariable Long sessionId,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "20") Integer pageSize
    ) {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer userId = (Integer) userMap.get("id");
        PageBean<ChatMessage> pb = chatService.getSessionMsg(sessionId, pageNum, pageSize, userId);
        return Result.success(pb);
    }

    /**
     * 发送消息
     */
    @PreAuthorize("/chat/send")
    @PostMapping("/send")
    public Result<ChatMessage> sendMsg(@RequestBody @Validated ChatMsgSendDTO dto) {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer senderId = (Integer) userMap.get("id");
        ChatMessage message = chatService.sendMsg(dto, senderId);
        return Result.success(message);
    }

    /**
     * 标记会话消息为已读
     */
    @PreAuthorize("/chat/markRead/{sessionId}")
    @PutMapping("/markRead/{sessionId}")
    public Result markAsRead(@PathVariable Long sessionId) {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer userId = (Integer) userMap.get("id");
        chatService.markMsgAsRead(sessionId, userId);
        return Result.success("标记已读成功");
    }


    /**
     * （可选扩展）创建会话（前端一般无需主动调用，发送消息时后端自动创建）
     * @param {Number} receiverId 接收者ID
     * @returns {Promise} 会话对象
     */
    @PreAuthorize("/chat/createSession/{receiverId}")
    @PostMapping("/createSession/{receiverId}")
    public Result<ChatSession> createSession(@PathVariable Integer receiverId) {
        Map<String, Object> userMap = ThreadLocalUtil.get();
        Integer senderId = (Integer) userMap.get("id");
        ChatSession session = chatService.createSession(senderId, receiverId);
        return Result.success(session);
    }
}