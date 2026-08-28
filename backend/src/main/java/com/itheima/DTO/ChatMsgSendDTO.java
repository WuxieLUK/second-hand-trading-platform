package com.itheima.DTO;

import lombok.Data;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

/**
 * 发送消息DTO
 */
@Data
public class ChatMsgSendDTO {
    @NotNull(message = "会话ID不能为空")
    private Long sessionId; // 会话ID（可选：有会话传会话ID，无会话传接收者ID）
    @NotNull(message = "接收者ID不能为空")
    private Integer receiverId; // 接收者ID
    @NotBlank(message = "消息内容不能为空")
    private String content; // 消息内容
    private Integer msgType = 1; // 消息类型，默认文本
}