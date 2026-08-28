package org.example.product.service;

import org.example.ai.POJO.AiChatSession;
import org.example.ai.POJO.AiChatMessage;
import org.example.ai.VO.AiMessageVO;
import org.example.ai.VO.AiSessionVO;

import java.util.List;

/**
 * AI 对话会话管理服务
 * 负责会话的CRUD和消息持久化
 */
public interface AiSessionService {

    /**
     * 创建新会话
     */
    AiChatSession createSession(Integer userId, String title);

    /**
     * 获取用户的会话列表（按更新时间倒序）
     */
    List<AiSessionVO> listSessions(Integer userId);

    /**
     * 删除会话及所有消息
     */
    void deleteSession(String sessionId, Integer userId);

    /**
     * 更新会话标题
     */
    void updateTitle(String sessionId, String title);

    /**
     * 保存消息
     */
    void saveMessage(String sessionId, String role, String content);

    /**
     * 批量保存消息
     */
    void saveMessages(List<AiChatMessage> messages);

    /**
     * 获取会话的所有消息
     */
    List<AiMessageVO> getMessages(String sessionId);

    /**
     * 获取最近的 N 条消息
     */
    List<AiChatMessage> getRecentMessages(String sessionId, int limit);

    /**
     * 获取或创建会话（如果会话不存在则创建）
     */
    AiChatSession getOrCreateSession(String sessionId, Integer userId);
}
