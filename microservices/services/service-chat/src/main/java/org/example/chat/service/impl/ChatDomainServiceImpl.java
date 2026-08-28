package org.example.chat.service.impl;

import com.github.pagehelper.Page;
import com.github.pagehelper.PageHelper;
import org.example.chat.DTO.ChatMsgSendDTO;
import org.example.chat.POJO.ChatMessage;
import org.example.chat.POJO.ChatSession;
import org.example.chat.feign.UserFeignClient;
import org.example.chat.mapper.ChatMessageMapper;
import org.example.chat.mapper.ChatSessionMapper;
import org.example.chat.service.ChatDomainService;
import org.example.chat.service.ChatPushGateway;
import org.example.common.PageBean;
import org.example.common.Result;
import org.example.user.POJO.User;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class ChatDomainServiceImpl implements ChatDomainService {

    private final ChatSessionMapper chatSessionMapper;
    private final ChatMessageMapper chatMessageMapper;
    private final UserFeignClient userFeignClient;
    private final ChatPushGateway chatPushGateway;

    public ChatDomainServiceImpl(ChatSessionMapper chatSessionMapper,
                                 ChatMessageMapper chatMessageMapper,
                                 UserFeignClient userFeignClient,
                                 ChatPushGateway chatPushGateway) {
        this.chatSessionMapper = chatSessionMapper;
        this.chatMessageMapper = chatMessageMapper;
        this.userFeignClient = userFeignClient;
        this.chatPushGateway = chatPushGateway;
    }

    @Override
    @Transactional(readOnly = true)
    public List<ChatSession> getMyChatList(Integer userId) {
        List<ChatSession> sessionList = chatSessionMapper.selectMyChatList(userId);
        Map<Integer, User> userMap = loadUsers(sessionList.stream()
                .map(session -> Objects.equals(session.getFromUserId(), userId) ? session.getToUserId() : session.getFromUserId())
                .distinct()
                .collect(Collectors.toList()));
        sessionList.forEach(session -> {
            Integer friendId = Objects.equals(session.getFromUserId(), userId) ? session.getToUserId() : session.getFromUserId();
            User friend = userMap.get(friendId);
            if (friend != null) {
                session.setFriendNickname(friend.getNickname());
                session.setFriendAvatar(friend.getUserPic());
            }
            session.setUnreadCount(Objects.equals(session.getFromUserId(), userId) ? session.getFromUnread() : session.getToUnread());
        });
        return sessionList;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public PageBean<ChatMessage> getSessionMsg(Long sessionId, Integer pageNum, Integer pageSize, Integer userId) {
        chatMessageMapper.markMsgAsRead(sessionId, userId);
        chatSessionMapper.clearUnread(sessionId, userId);
        PageHelper.startPage(pageNum, pageSize);
        List<ChatMessage> messages = chatMessageMapper.selectMsgBySession(sessionId);
        Map<Integer, User> userMap = loadUsers(messages.stream().map(ChatMessage::getSenderId).distinct().collect(Collectors.toList()));
        messages.forEach(message -> {
            User sender = userMap.get(message.getSenderId());
            if (sender != null) {
                message.setSenderNickname(sender.getNickname());
                message.setSenderAvatar(sender.getUserPic());
            }
        });
        Page<ChatMessage> page = (Page<ChatMessage>) messages;
        PageBean<ChatMessage> pageBean = new PageBean<>();
        pageBean.setTotal(page.getTotal());
        pageBean.setItems(page.getResult());
        return pageBean;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatMessage sendMsg(ChatMsgSendDTO dto, Integer senderId) {
        User sender = userFeignClient.getUserById(senderId).getData();
        User receiver = userFeignClient.getUserById(dto.getReceiverId()).getData();
        if (sender == null || receiver == null) {
            throw new IllegalArgumentException("聊天用户不存在");
        }
        ChatSession session = createSession(senderId, dto.getReceiverId());
        ChatMessage message = new ChatMessage();
        message.setSessionId(session.getId());
        message.setSenderId(senderId);
        message.setReceiverId(dto.getReceiverId());
        message.setContent(dto.getContent());
        message.setMsgType(dto.getMsgType());
        chatMessageMapper.insertMessage(message);
        chatSessionMapper.updateSessionLastMsg(session.getId(), dto.getContent(), dto.getReceiverId());
        message.setSenderNickname(sender.getNickname());
        message.setSenderAvatar(sender.getUserPic());
        chatPushGateway.pushToUser(dto.getReceiverId(), message);
        return message;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void markMsgAsRead(Long sessionId, Integer userId) {
        chatMessageMapper.markMsgAsRead(sessionId, userId);
        chatSessionMapper.clearUnread(sessionId, userId);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ChatSession createSession(Integer senderId, Integer receiverId) {
        if (userFeignClient.getUserById(receiverId).getData() == null) {
            throw new IllegalArgumentException("接收用户不存在");
        }
        ChatSession session = chatSessionMapper.selectSessionByTwoUser(senderId, receiverId);
        if (session == null) {
            session = new ChatSession();
            session.setFromUserId(senderId);
            session.setToUserId(receiverId);
            session.setLastMsg("");
            chatSessionMapper.insertSession(session);
        }
        return session;
    }

    private Map<Integer, User> loadUsers(List<Integer> userIds) {
        if (userIds == null || userIds.isEmpty()) {
            return Map.of();
        }
        Result<List<User>> result = userFeignClient.getUsersByIds(userIds);
        List<User> users = result.getData();
        if (users == null) {
            return Map.of();
        }
        return users.stream().collect(Collectors.toMap(User::getId, user -> user, (a, b) -> a));
    }
}
