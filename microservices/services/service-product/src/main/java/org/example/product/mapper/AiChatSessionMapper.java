package org.example.product.mapper;

import org.apache.ibatis.annotations.Mapper;
import org.example.ai.POJO.AiChatSession;

import java.util.List;

/**
 * AI 对话会话 Mapper
 */
@Mapper
public interface AiChatSessionMapper {

    void insert(AiChatSession session);

    void update(AiChatSession session);

    void deleteById(String id);

    void deleteByUserId(Integer userId);

    AiChatSession findById(String id);

    List<AiChatSession> findByUserId(Integer userId);
}
