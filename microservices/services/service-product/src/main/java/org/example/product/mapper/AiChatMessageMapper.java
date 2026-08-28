package org.example.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.ai.POJO.AiChatMessage;

import java.util.List;

/**
 * AI 对话消息 Mapper
 */
@Mapper
public interface AiChatMessageMapper {

    void insert(AiChatMessage message);

    void insertBatch(List<AiChatMessage> messages);

    void deleteBySessionId(String sessionId);

    List<AiChatMessage> findBySessionId(String sessionId);

    List<AiChatMessage> findRecentBySessionId(String sessionId, Integer limit);

    Integer countBySessionId(String sessionId);
}
