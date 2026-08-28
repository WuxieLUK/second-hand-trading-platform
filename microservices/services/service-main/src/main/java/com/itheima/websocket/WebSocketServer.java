package com.itheima.websocket;

import lombok.extern.slf4j.Slf4j;

/**
 * WebSocket服务端（已废弃，改用 WsHandler + Spring WebSocket）
 *
 * 前端 WebSocket URL 已改为 query 参数方式，由 WsHandshakeInterceptor
 * 拦截校验 token，WsHandler 处理消息收发。
 * 此类保留为空桩，避免 git 历史断裂，可安全删除。
 */
@Slf4j
public class WebSocketServer {

    private WebSocketServer() {
        // 工具类，禁止实例化
    }

    /**
     * 检查 WebSocket 相关组件是否加载
     */
    public static boolean isAvailable() {
        return false;
    }
}
