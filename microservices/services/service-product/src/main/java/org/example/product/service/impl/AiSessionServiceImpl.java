package org.example.product.service.impl;

import lombok.extern.slf4j.Slf4j;
import org.example.ai.POJO.AiChatMessage;
import org.example.ai.POJO.AiChatSession;
import org.example.ai.VO.AiMessageVO;
import org.example.ai.VO.AiSessionVO;
import org.example.product.mapper.AiChatMessageMapper;
import org.example.product.mapper.AiChatSessionMapper;
import org.example.product.service.AiSessionService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * AI 对话会话管理服务实现
 */
@Slf4j
@Service
public class AiSessionServiceImpl implements AiSessionService {

    private final AiChatSessionMapper sessionMapper;
    private final AiChatMessageMapper messageMapper;

    public AiSessionServiceImpl(AiChatSessionMapper sessionMapper,
                                AiChatMessageMapper messageMapper) {
        this.sessionMapper = sessionMapper;
        this.messageMapper = messageMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSession createSession(Integer userId, String title) {
        AiChatSession session = new AiChatSession();
        session.setId(UUID.randomUUID().toString());
        session.setUserId(userId);
        session.setTitle(title != null && !title.isBlank() ? title : "新对话");
        session.setMessageCount(0);
        LocalDateTime now = LocalDateTime.now();
        session.setCreateTime(now);
        session.setUpdateTime(now);
        sessionMapper.insert(session);
        log.info("创建AI对话会话: sessionId={}, userId={}", session.getId(), userId);
        return session;
    }

    @Override
    public List<AiSessionVO> listSessions(Integer userId) {
        List<AiChatSession> sessions = sessionMapper.findByUserId(userId);
        return sessions.stream().map(s -> {
            AiSessionVO vo = new AiSessionVO();
            vo.setId(s.getId());
            vo.setTitle(s.getTitle());
            vo.setMessageCount(s.getMessageCount());
            vo.setCreateTime(s.getCreateTime());
            vo.setUpdateTime(s.getUpdateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void deleteSession(String sessionId, Integer userId) {
        AiChatSession session = sessionMapper.findById(sessionId);
        if (session != null && session.getUserId().equals(userId)) {
            messageMapper.deleteBySessionId(sessionId);
            sessionMapper.deleteById(sessionId);
            log.info("删除AI对话会话: sessionId={}, userId={}", sessionId, userId);
        }
    }

    @Override
    public void updateTitle(String sessionId, String title) {
        AiChatSession session = sessionMapper.findById(sessionId);
        if (session != null) {
            session.setTitle(title);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.update(session);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessage(String sessionId, String role, String content) {
        Integer count = messageMapper.countBySessionId(sessionId);
        int seq = (count != null ? count : 0) + 1;

        AiChatMessage message = new AiChatMessage();
        message.setSessionId(sessionId);
        message.setRole(role);
        message.setContent(content);
        message.setSequence(seq);
        message.setCreateTime(LocalDateTime.now());
        messageMapper.insert(message);

        AiChatSession session = sessionMapper.findById(sessionId);
        if (session != null) {
            session.setMessageCount(seq);
            session.setUpdateTime(LocalDateTime.now());
            sessionMapper.update(session);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveMessages(List<AiChatMessage> messages) {
        if (messages == null || messages.isEmpty()) return;
        messageMapper.insertBatch(messages);
    }

    @Override
    public List<AiMessageVO> getMessages(String sessionId) {
        List<AiChatMessage> messages = messageMapper.findBySessionId(sessionId);
        return messages.stream().map(m -> {
            AiMessageVO vo = new AiMessageVO();
            vo.setId(m.getId());
            vo.setSessionId(m.getSessionId());
            vo.setRole(m.getRole());
            vo.setContent(m.getContent());
            vo.setSequence(m.getSequence());
            vo.setCreateTime(m.getCreateTime());
            return vo;
        }).collect(Collectors.toList());
    }

    @Override
    public List<AiChatMessage> getRecentMessages(String sessionId, int limit) {
        return messageMapper.findRecentBySessionId(sessionId, limit);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public AiChatSession getOrCreateSession(String sessionId, Integer userId) {
        // 1. 如果传入了 sessionId 且 DB 中存在，直接返回
        if (sessionId != null && !sessionId.isBlank()) {
            AiChatSession existing = sessionMapper.findById(sessionId);
            if (existing != null) {
                return existing;
            }
            // 2. sessionId 存在但 DB 中没有 → 用这个 ID 创建新记录
            //    这样前端生成的 sessionId 会被直接使用，避免 ID 不一致
            AiChatSession session = new AiChatSession();
            session.setId(sessionId);
            session.setUserId(userId);
            session.setTitle("新对话");
            session.setMessageCount(0);
            LocalDateTime now = LocalDateTime.now();
            session.setCreateTime(now);
            session.setUpdateTime(now);
            sessionMapper.insert(session);
            log.info("使用前端提供的ID创建会话: sessionId={}, userId={}", sessionId, userId);
            return session;
        }
        // 3. 没有 sessionId → 自动生成
        return createSession(userId, "新对话");
    }
}
