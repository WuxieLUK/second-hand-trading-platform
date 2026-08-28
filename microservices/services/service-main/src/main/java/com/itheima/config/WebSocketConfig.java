package com.itheima.config;

import com.itheima.websocket.WsHandshakeInterceptor;
import com.itheima.websocket.WsHandler;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.socket.config.annotation.EnableWebSocket;
import org.springframework.web.socket.config.annotation.WebSocketConfigurer;
import org.springframework.web.socket.config.annotation.WebSocketHandlerRegistry;

@Configuration
@EnableWebSocket
@RequiredArgsConstructor
public class WebSocketConfig implements WebSocketConfigurer {

    private final WsHandler wsHandler;
    private final WsHandshakeInterceptor handshakeInterceptor;

    @Override
    public void registerWebSocketHandlers(WebSocketHandlerRegistry registry) {
        registry.addHandler(wsHandler, "/ws/chat")
                .addInterceptors(handshakeInterceptor)
                .setAllowedOrigins("*");
    }
}