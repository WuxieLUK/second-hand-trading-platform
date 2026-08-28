package com.itheima.mapper;

import com.itheima.pojo.ChatSession;
import org.apache.ibatis.annotations.*;

import java.util.List;

@Mapper
public interface ChatSessionMapper {
    /**
     * 查询我的所有会话（核心：和我相关的所有聊天对象）
     */
    @Select("""
        SELECT cs.*, 
               u.nickname AS friendNickname, 
               u.user_pic AS friendAvatar
        FROM chat_session cs
        LEFT JOIN user u ON 
            (cs.from_user_id = #{userId} AND u.id = cs.to_user_id) 
            OR (cs.to_user_id = #{userId} AND u.id = cs.from_user_id)
        WHERE cs.from_user_id = #{userId} OR cs.to_user_id = #{userId}
        AND cs.status = 1
        ORDER BY cs.last_msg_time DESC
    """)
    List<ChatSession> selectMyChatList(Integer userId);

    /**
     * 根据两个用户ID查询会话（适配user_pair_min/max唯一索引）
     */
    @Select("""
        SELECT * FROM chat_session
        WHERE user_pair_min = LEAST(#{userId1}, #{userId2}) 
          AND user_pair_max = GREATEST(#{userId1}, #{userId2})
        AND status = 1
    """)
    ChatSession selectSessionByTwoUser(@Param("userId1") Integer userId1, @Param("userId2") Integer userId2);

    /**
     * 创建会话（计算列无需赋值）
     */
    @Insert("""
        INSERT INTO chat_session (from_user_id, to_user_id, last_msg, last_msg_time)
        VALUES (#{fromUserId}, #{toUserId}, #{lastMsg}, NOW())
    """)
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertSession(ChatSession session);

    /**
     * 更新会话最后一条消息+未读数
     */
    @Update("""
        UPDATE chat_session 
        SET last_msg = #{lastMsg}, last_msg_time = NOW(),
            from_unread = CASE WHEN to_user_id = #{receiveUserId} THEN from_unread + 1 ELSE from_unread END,
            to_unread = CASE WHEN from_user_id = #{receiveUserId} THEN to_unread + 1 ELSE to_unread END
        WHERE id = #{sessionId}
    """)
    void updateSessionLastMsg(@Param("sessionId") Long sessionId, @Param("lastMsg") String lastMsg, @Param("receiveUserId") Integer receiveUserId);

    /**
     * 清空会话未读数
     */
    @Update("""
        UPDATE chat_session 
        SET from_unread = CASE WHEN from_user_id = #{userId} THEN 0 ELSE from_unread END,
            to_unread = CASE WHEN to_user_id = #{userId} THEN 0 ELSE to_unread END
        WHERE id = #{sessionId}
    """)
    void clearUnread(@Param("sessionId") Long sessionId, @Param("userId") Integer userId);
}