package org.example.chat.mapper;

import org.apache.ibatis.annotations.*;
import org.example.chat.POJO.ChatSession;

import java.util.List;

@Mapper
public interface ChatSessionMapper {

    @Select("SELECT * FROM chat_session WHERE (from_user_id = #{userId} OR to_user_id = #{userId}) AND status = 1 ORDER BY last_msg_time DESC")
    List<ChatSession> selectMyChatList(Integer userId);

    @Select("SELECT * FROM chat_session WHERE user_pair_min = LEAST(#{userId1}, #{userId2}) AND user_pair_max = GREATEST(#{userId1}, #{userId2}) AND status = 1")
    ChatSession selectSessionByTwoUser(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    @Insert("INSERT INTO chat_session (from_user_id, to_user_id, last_msg, last_msg_time, status) VALUES (#{fromUserId}, #{toUserId}, #{lastMsg}, NOW(), 1)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSession(ChatSession session);

    @Update("""
        UPDATE chat_session
        SET last_msg = #{lastMsg}, last_msg_time = NOW(),
            from_unread = CASE WHEN to_user_id = #{receiveUserId} THEN from_unread + 1 ELSE from_unread END,
            to_unread = CASE WHEN from_user_id = #{receiveUserId} THEN to_unread + 1 ELSE to_unread END
        WHERE id = #{sessionId}
        """)
    void updateSessionLastMsg(@Param("sessionId") Long sessionId, @Param("lastMsg") String lastMsg, @Param("receiveUserId") Integer receiveUserId);

    @Update("""
        UPDATE chat_session
        SET from_unread = CASE WHEN from_user_id = #{userId} THEN 0 ELSE from_unread END,
            to_unread = CASE WHEN to_user_id = #{userId} THEN 0 ELSE to_unread END
        WHERE id = #{sessionId}
        """)
    void clearUnread(@Param("sessionId") Long sessionId, @Param("userId") Integer userId);
}
