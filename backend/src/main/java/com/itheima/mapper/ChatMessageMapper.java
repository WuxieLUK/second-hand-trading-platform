package com.itheima.mapper;

import com.itheima.pojo.ChatMessage;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatMessageMapper {
    /**
     * 发送消息（插入消息表）
     */
    @Insert("""
        INSERT INTO chat_message (session_id, sender_id, receiver_id, content, msg_type, is_read)
        VALUES (#{sessionId}, #{senderId}, #{receiverId}, #{content}, #{msgType}, 0)
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMessage(ChatMessage message);

    /**
     * 查询会话历史消息（修复：移除手动 LIMIT，交给 PageHelper 处理）
     */
    @Select("""
        SELECT cm.*, u.nickname AS senderNickname, u.user_pic AS senderAvatar
        FROM chat_message cm
        LEFT JOIN user u ON cm.sender_id = u.id
        WHERE cm.session_id = #{sessionId}
        ORDER BY cm.create_time ASC
    """)
    List<ChatMessage> selectMsgBySession(@Param("sessionId") Long sessionId);

    /**
     * 标记消息为已读
     */
    @Update("""
        UPDATE chat_message 
        SET is_read = 1 
        WHERE session_id = #{sessionId} 
          AND receiver_id = #{userId} 
          AND is_read = 0
    """)
    void markMsgAsRead(@Param("sessionId") Long sessionId, @Param("userId") Integer userId);

    /**
     * 查询会话消息总数
     */
    @Select("SELECT COUNT(*) FROM chat_message WHERE session_id = #{sessionId}")
    Integer countMsgBySession(Long sessionId);
}