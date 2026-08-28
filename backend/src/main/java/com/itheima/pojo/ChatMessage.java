package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天消息实体（对应chat_message表）
 */
@Data
public class ChatMessage {
    private Long id; // 消息ID
    private Long sessionId; // 会话ID
    private Integer senderId; // 发送者ID
    private Integer receiverId; // 接收者ID
    private String content; // 消息内容
    private Integer msgType; // 消息类型：1-文本 2-图片 3-语音
    private Integer isRead; // 是否已读：0-未读 1-已读
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 发送时间

    // 扩展字段
    private String senderNickname; // 发送者昵称
    private String senderAvatar; // 发送者头像
}