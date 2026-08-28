package org.example.ai.VO;

import java.time.LocalDateTime;

/**
 * AI 会话视图对象（返回给前端）
 */
public class AiSessionVO {

    private String id;
    private String title;
    private Integer messageCount;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;

    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }

    public Integer getMessageCount() { return messageCount; }
    public void setMessageCount(Integer messageCount) { this.messageCount = messageCount; }

    public LocalDateTime getCreateTime() { return createTime; }
    public void setCreateTime(LocalDateTime createTime) { this.createTime = createTime; }

    public LocalDateTime getUpdateTime() { return updateTime; }
    public void setUpdateTime(LocalDateTime updateTime) { this.updateTime = updateTime; }
}
