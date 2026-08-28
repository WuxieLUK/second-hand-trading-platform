package com.itheima.websocket;

import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.WebSocketHandler;
import org.springframework.web.socket.server.HandshakeInterceptor;

import java.util.Map;

/**
 * WebSocket 握手拦截器（已改为首次消息鉴权模式）
 *
 * 原先在此处校验 token，现在放行所有握手请求，
 * token 校验移至 WsHandler.handleTextMessage，由客户端第一条消息完成鉴权。
 * 避免 token 出现在 URL 中（防止日志泄露、浏览器历史记录等安全风险）。
 */
@Component
public class WsHandshakeInterceptor implements HandshakeInterceptor {

    @Override
    public boolean beforeHandshake(ServerHttpRequest request, ServerHttpResponse response,
                                   WebSocketHandler wsHandler, Map<String, Object> attributes) {
        // 放行所有握手请求，后续通过第一条消息鉴权
        return true;
    }

    @Override
    public void afterHandshake(ServerHttpRequest request, ServerHttpResponse response,
                               WebSocketHandler wsHandler, Exception exception) {
    }
}
