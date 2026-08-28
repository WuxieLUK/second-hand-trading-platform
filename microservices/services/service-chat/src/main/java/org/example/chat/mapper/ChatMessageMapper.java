package org.example.chat.mapper;

import org.apache.ibatis.annotations.*;
import org.example.chat.POJO.ChatMessage;

import java.util.List;

@Mapper
public interface ChatMessageMapper {

    @Insert("INSERT INTO chat_message (session_id, sender_id, receiver_id, content, msg_type, is_read) VALUES (#{sessionId}, #{senderId}, #{receiverId}, #{content}, #{msgType}, 0)")
    @Options(useGeneratedKeys = true, keyProperty = "id")
    void insertMessage(ChatMessage message);

    @Select("SELECT * FROM chat_message WHERE session_id = #{sessionId} ORDER BY create_time ASC")
    List<ChatMessage> selectMsgBySession(@Param("sessionId") Long sessionId);

    @Update("UPDATE chat_message SET is_read = 1 WHERE session_id = #{sessionId} AND receiver_id = #{userId} AND is_read = 0")
    void markMsgAsRead(@Param("sessionId") Long sessionId, @Param("userId") Integer userId);
}
