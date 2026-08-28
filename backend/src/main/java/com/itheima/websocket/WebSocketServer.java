package com.itheima.websocket;

import com.alibaba.fastjson.JSON;
import com.itheima.pojo.ChatMessage;
import com.itheima.util.JwtUtil;
import com.itheima.util.ThreadLocalUtil;
import jakarta.annotation.Resource;
import jakarta.websocket.*;
import jakarta.websocket.server.PathParam;
import jakarta.websocket.server.ServerEndpoint;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * WebSocket服务端（适配现有登录拦截器的Token校验规则）
 */
@ServerEndpoint("/ws/chat/{userId}/{token}")
@Component
@Slf4j
public class WebSocketServer {
    // 在线人数统计（原子类保证并发安全）
    private static final AtomicInteger ONLINE_COUNT = new AtomicInteger(0);
    // 保存所有在线连接：key=用户ID，value=会话对象
    private static final ConcurrentHashMap<String, Session> ONLINE_USERS = new ConcurrentHashMap<>();

    // 静态注入现有工具类（适配WebSocket多实例特性）
    private static StringRedisTemplate stringRedisTemplate;
    // 使用setter方法注入（解决静态注入问题）
    @Resource
    public void setStringRedisTemplate(StringRedisTemplate stringRedisTemplate) {
        WebSocketServer.stringRedisTemplate = stringRedisTemplate;
    }

    /**
     * 连接建立成功调用（三次握手完成后触发）
     */
    @OnOpen
    public void onOpen(Session session,
                       @PathParam("userId") String userId,
                       @PathParam("token") String token) {
        // ======== 核心：复用登录拦截器的Token校验逻辑 ========
        try {
            if (token == null || token.isEmpty()) {
                log.warn("用户{}建立WebSocket连接时未携带Token，拒绝连接", userId);
                throw new RuntimeException("未携带Token");
            }

            // 1. 从Redis中校验token是否存在（和拦截器逻辑一致）
            ValueOperations<String, String> operations = stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if (redisToken == null) {
                log.warn("用户{}的Token已失效（Redis中不存在），拒绝WebSocket连接", userId);
                throw new RuntimeException("Token已失效");
            }

            // 2. 校验token一致性（和拦截器逻辑一致）
            if (!token.equals(redisToken)) {
                log.warn("用户{}的Token不一致，拒绝WebSocket连接", userId);
                throw new RuntimeException("Token不一致");
            }

            // 3. 解析并验证token（和拦截器逻辑一致）
            Map<String, Object> claims = JwtUtil.parseToken(token);
            String tokenUserId = claims.get("id").toString(); // 请根据实际JWT存储的字段名调整
            // 验证token中的用户ID与路径中的userId是否一致
            if (tokenUserId == null || !tokenUserId.equals(userId)) {
                log.warn("Token中的用户ID({})与请求的用户ID({})不一致，拒绝WebSocket连接", tokenUserId, userId);
                throw new RuntimeException("用户ID不匹配");
            }

            // 可选：将claims存入ThreadLocal（如果后续业务需要）
            ThreadLocalUtil.set(claims);

        } catch (Exception e) {
            // 校验失败，关闭连接
            closeSession(session, e.getMessage());
            return;
        }
        // =====================================

        // 并发安全地添加会话
        ONLINE_USERS.putIfAbsent(userId, session);
        // 在线人数+1
        int count = ONLINE_COUNT.incrementAndGet();
        log.info("用户{}建立WebSocket连接，当前在线人数：{}", userId, count);

        // 可选：发送心跳检测（防止Nginx/网关断开空闲连接）
        sendHeartbeat(session, userId);
    }

    // 关闭会话的工具方法（带错误信息）
    private void closeSession(Session session, String errorMsg) {
        try {
            // 发送关闭原因给客户端
            session.getBasicRemote().sendText(JSON.toJSONString(new ChatMessage() {{
                setMsgType(-1); // -1-认证失败
                setContent(errorMsg == null ? "Token无效或已过期，请重新登录" : errorMsg);
            }}));
            session.close(new CloseReason(CloseReason.CloseCodes.VIOLATED_POLICY, errorMsg));
        } catch (IOException e) {
            log.error("关闭未认证会话失败：", e);
        } finally {
            // 清理ThreadLocal，防止内存泄漏
            ThreadLocalUtil.remove();
        }
    }

    /**
     * 连接关闭调用（四次挥手完成后触发）
     */
    @OnClose
    public void onClose(@PathParam("userId") String userId) {
        // 清理ThreadLocal
        ThreadLocalUtil.remove();
        // 移除会话
        ONLINE_USERS.remove(userId);
        // 在线人数-1
        int count = ONLINE_COUNT.decrementAndGet();
        log.info("用户{}关闭WebSocket连接，当前在线人数：{}", userId, count);
    }

    /**
     * 收到客户端消息调用（前端主动发消息时）
     * 扩展：支持前端直接通过WebSocket发消息，无需走HTTP接口
     */
    @OnMessage
    public void onMessage(String message, Session session, @PathParam("userId") String senderId) {
        log.info("收到用户{}的消息：{}", senderId, message);
        try {
            // 解析前端发送的消息（格式：{receiverId: 200, content: "你好"}）
            ChatMessage msg = JSON.parseObject(message, ChatMessage.class);
            // 转发给接收者
            sendToUser(msg.getReceiverId().toString(), msg);
        } catch (Exception e) {
            log.error("转发客户端消息失败：", e);
        }
    }

    /**
     * 发生错误调用
     */
    @OnError
    public void onError(Session session, Throwable error) {
        log.error("WebSocket连接错误：", error);
        // 清理ThreadLocal
        ThreadLocalUtil.remove();
        // 错误时主动关闭连接
        if (session.isOpen()) {
            try {
                session.close();
            } catch (IOException e) {
                log.error("关闭错误连接失败：", e);
            }
        }
    }

    /**
     * 发送消息给指定用户（核心：推送给接收者）
     */
    public void sendToUser(String userId, ChatMessage message) {
        Session session = ONLINE_USERS.get(userId);
        if (session == null) {
            log.warn("用户{}未在线，无法推送消息", userId);
            // 可选：离线消息存储（存入数据库，用户上线后推送）
            return;
        }
        if (!session.isOpen()) {
            log.warn("用户{}的WebSocket连接已关闭，移除会话", userId);
            ONLINE_USERS.remove(userId);
            ONLINE_COUNT.decrementAndGet();
            return;
        }
        try {
            // 线程安全地发送消息
            synchronized (session) {
                session.getBasicRemote().sendText(JSON.toJSONString(message));
            }
            log.info("成功推送消息给用户{}：{}", userId, message.getContent());
        } catch (Exception e) {
            log.error("推送消息给用户{}失败：", userId, e);
        }
    }

    /**
     * 心跳检测（防止Nginx/网关断开空闲连接）
     */
    private void sendHeartbeat(Session session, String userId) {
        new Thread(() -> {
            while (session.isOpen()) {
                try {
                    // 每 30 秒发送一次心跳包
                    Thread.sleep(30000);

                    // 再次检查 session 状态，防止在 sleep 期间被关闭
                    if (!session.isOpen()) {
                        break;
                    }

                    synchronized (session) {
                        session.getBasicRemote().sendText(JSON.toJSONString(new ChatMessage() {{
                            setMsgType(0); // 0-心跳包
                            setContent("ping");
                        }}));
                    }
                } catch (IllegalStateException e) {
                    // Session 已关闭，停止心跳线程
                    log.warn("用户{}的 WebSocket 会话已关闭，停止心跳检测", userId);
                    break;
                } catch (InterruptedException e) {
                    // 线程被中断，停止心跳
                    log.warn("用户{}的心跳线程被中断", userId, e);
                    Thread.currentThread().interrupt();
                    break;
                } catch (Exception e) {
                    log.error("给用户{}发送心跳包失败：", userId, e);
                    // 遇到其他异常也停止心跳，避免无限循环
                    break;
                }
            }
        }).start();
    }

    /**
     * 广播消息（可选：如系统通知）
     */
    public void broadcast(ChatMessage message) {
        String jsonMsg = JSON.toJSONString(message);
        ONLINE_USERS.forEach((userId, session) -> {
            if (session.isOpen()) {
                try {
                    synchronized (session) {
                        session.getBasicRemote().sendText(jsonMsg);
                    }
                } catch (Exception e) {
                    log.error("广播消息给用户{}失败：", userId, e);
                }
            }
        });
    }
}