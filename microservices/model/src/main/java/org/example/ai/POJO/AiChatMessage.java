package org.example.ai.POJO;

import java.time.LocalDateTime;

/**
 * AI 对话消息
 * 存储AI对话中的每条消息（用户消息 + AI回复）
 */
public class AiChatMessage {

    /** 消息ID */
    private Long id;

    /** 会话ID */
    private String sessionId;

    /** 角色：user / ai */
    private String role;

    /** 消息内容 */
    private String content;

    /** 消息序号（用于顺序排列） */
    private Integer sequence;

    /** 创建时间 */
    private LocalDateTime createTime;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }

    public String getSessionId() { return sessionId; }
    public void setSessionId(String sessionId) { this.sessionId = sessionId; }

    public String getRole() { return role; }
    public void setRole(String role) { this.role = role; }

    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }

    public Integer getSequence() { return sequence; }
    public void setSequence(Integer sequence) { this.sequence = sequence; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }
}
