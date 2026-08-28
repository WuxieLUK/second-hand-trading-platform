package com.itheima.pojo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import java.time.LocalDateTime;

/**
 * 聊天会话实体（适配修改后的chat_session表）
 */
@Data
public class ChatSession {
    private Long id; // 会话ID
    private Integer fromUserId; // 发起方用户ID
    private Integer toUserId; // 接收方用户ID
    // 计算列：无需手动赋值，仅用于查询/展示
    private Integer userPairMin;
    private Integer userPairMax;
    private String lastMsg; // 最后一条消息
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime lastMsgTime; // 最后一条消息时间
    private Integer fromUnread; // 发起方未读数
    private Integer toUnread; // 接收方未读数
    private Integer status; // 会话状态：1-正常 2-已拉黑
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime; // 创建时间
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime updateTime; // 更新时间

    // 扩展字段（前端展示用）
    private String friendNickname; // 聊天对象昵称
    private String friendAvatar; // 聊天对象头像
    private Integer unreadCount; // 当前用户的未读数（前端核心）
}