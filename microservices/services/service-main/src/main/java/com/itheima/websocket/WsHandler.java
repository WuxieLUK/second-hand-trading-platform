package com.itheima.websocket;

import com.alibaba.fastjson.JSON;
import com.itheima.util.JwtUtil;
import com.itheima.util.ThreadLocalUtil;
import jakarta.annotation.Resource;
import lombok.extern.slf4j.Slf4j;
import org.example.chat.POJO.ChatMessage;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.AbstractWebSocketHandler;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket 处理器（首次消息鉴权模式）
 *
 * 鉴权流程：
 * 1. 客户端建立连接（URL 中不携带 token）
 * 2. 客户端发送第一条消息：{ msgType: -2, content: "JWT_TOKEN" }
 * 3. 服务端校验 token，通过后将 session 加入在线列表
 * 4. 鉴权失败则关闭连接
 *
 * 优势：token 不会出现在 URL、服务器日志、浏览器历史中
 */
@Slf4j
@Component
public class WsHandler extends AbstractWebSocketHandler {

    private static final Map<Integer, WebSocketSession> USER_SESSION_MAP = new ConcurrentHashMap<>();
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);

    /** 鉴权消息类型 */
    private static final int MSG_TYPE_AUTH = -2;
    /** 心跳消息类型 */
    private static final int MSG_TYPE_PING = 0;

    private static StringRedisTemplate stringRedisTemplate;

    @Resource
    public void setStringRedisTemplate(StringRedisTemplate redisTemplate) {
        WsHandler.stringRedisTemplate = redisTemplate;
    }

    // ==================== 生命周期 ====================

    @Override
    public void afterConnectionEstablished(WebSocketSession session) {
        log.info("WebSocket 连接建立，等待鉴权... 会话ID: {}", session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) {
        try {
            Boolean authenticated = (Boolean) session.getAttributes().get("authenticated");

            // ---- 未鉴权：处理鉴权消息 ----
            if (authenticated == null || !authenticated) {
                handleAuth(session, message);
                return;
            }

            // ---- 已鉴权：处理业务消息 ----
            Integer senderId = (Integer) session.getAttributes().get("userId");
            ChatMessage msg = JSON.parseObject(message.getPayload(), ChatMessage.class);
            sendToUser(msg.getReceiverId(), msg);
        } catch (Exception e) {
            log.error("消息处理失败", e);
        }
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) {
        Integer userId = (Integer) session.getAttributes().get("userId");
        if (userId != null) {
            USER_SESSION_MAP.remove(userId);
            ONLINE_COUNT.decrementAndGet();
            log.info("用户{}断开连接，在线人数：{}", userId, ONLINE_COUNT.get());
        }
        ThreadLocalUtil.remove();
    }

    @Override
    public void handleTransportError(WebSocketSession session, Throwable exception) {
        log.error("ws异常", exception);
        closeSession(session);
    }

    // ==================== 鉴权逻辑 ====================

    /**
     * 处理鉴权消息：客户端连接后发送的第一条消息必须为 { msgType: -2, content: "JWT_TOKEN" }
     */
    private void handleAuth(WebSocketSession session, TextMessage message) {
        try {
            ChatMessage authMsg = JSON.parseObject(message.getPayload(), ChatMessage.class);
            if (authMsg.getMsgType() == null || authMsg.getMsgType() != MSG_TYPE_AUTH) {
                log.warn("期望鉴权消息，但收到 msgType={}，关闭连接", authMsg.getMsgType());
                sendAndClose(session, "请先发送鉴权消息");
                return;
            }

            String token = authMsg.getContent();
            if (token == null || token.isBlank()) {
                sendAndClose(session, "Token 不能为空");
                return;
            }

            // 1. 校验 Redis 中 token 是否存在
            ValueOperations<String, String> ops = stringRedisTemplate.opsForValue();
            String redisToken = ops.get(token);
            if (redisToken == null) {
                sendAndClose(session, "Token 已失效，请重新登录");
                return;
            }

            // 2. 校验 token 一致性
            if (!token.equals(redisToken)) {
                sendAndClose(session, "Token 不一致");
                return;
            }

            // 3. 解析 JWT
            Map<String, Object> claims = JwtUtil.parseToken(token);
            Integer userId = Integer.valueOf(claims.get("id").toString());

            // 4. 鉴权通过，设置 session 属性
            session.getAttributes().put("authenticated", true);
            session.getAttributes().put("userId", userId);
            ThreadLocalUtil.set(claims);

            // 5. 加入在线列表
            USER_SESSION_MAP.put(userId, session);
            ONLINE_COUNT.incrementAndGet();
            log.info("用户{}鉴权成功，在线人数：{}", userId, ONLINE_COUNT.get());

            // 6. 通知客户端鉴权成功
            ChatMessage ack = new ChatMessage();
            ack.setMsgType(MSG_TYPE_AUTH);
            ack.setContent("auth_ok");
            synchronized (session) {
                session.sendMessage(new TextMessage(JSON.toJSONString(ack)));
            }
        } catch (Exception e) {
            log.error("鉴权失败", e);
            sendAndClose(session, "鉴权失败: " + e.getMessage());
        }
    }

    // ==================== 消息推送 ====================

    public void sendToUser(Integer userId, ChatMessage message) {
        WebSocketSession session = USER_SESSION_MAP.get(userId);
        if (session == null || !session.isOpen()) return;

        try {
            synchronized (session) {
                session.sendMessage(new TextMessage(JSON.toJSONString(message)));
            }
        } catch (Exception e) {
            log.error("推送失败", e);
        }
    }

    // ==================== 心跳 ====================

    @Scheduled(fixedRate = 30000)
    public void heartbeat() {
        USER_SESSION_MAP.forEach((uid, session) -> {
            try {
                if (session.isOpen()) {
                    ChatMessage ping = new ChatMessage();
                    ping.setMsgType(MSG_TYPE_PING);
                    ping.setContent("ping");
                    session.sendMessage(new TextMessage(JSON.toJSONString(ping)));
                }
            } catch (Exception ignored) {}
        });
    }

    // ==================== 工具方法 ====================

    private void closeSession(WebSocketSession session) {
        try {
            if (session.isOpen()) session.close();
        } catch (Exception ignored) {}
    }

    /** 发送错误消息后关闭连接 */
    private void sendAndClose(WebSocketSession session, String reason) {
        try {
            if (session.isOpen()) {
                ChatMessage err = new ChatMessage();
                err.setMsgType(MSG_TYPE_AUTH);
                err.setContent("auth_fail");
                session.sendMessage(new TextMessage(JSON.toJSONString(err)));
                session.close(CloseStatus.POLICY_VIOLATION);
            }
        } catch (IOException e) {
            log.warn("发送鉴权失败消息异常", e);
        }
    }
}
