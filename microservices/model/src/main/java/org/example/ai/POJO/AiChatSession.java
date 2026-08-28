package org.example.ai.POJO;

import java.time.LocalDateTime;

/**
 * AI 对话会话
 * 存储每次AI对话的session信息，支持多会话管理和长对话记忆
 */
public class AiChatSession {

    /** 会话ID（UUID） */
    private String id;

    /** 用户ID */
    private Integer userId;

    /** 会话标题（自动取第一条用户消息） */
    private String title;

    /** 消息总数 */
    private Integer messageCount;

    /** 创建时间 */
    private LocalDateTime createTime;

    /** 最后活动时间 */
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public Integer getUserId() { return userId; }
    public void setUserId(Integer userId) { this.userId = userId; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
