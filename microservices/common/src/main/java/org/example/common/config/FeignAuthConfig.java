package org.example.common.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

/**
 * Feign 认证头传播配置（统一所有服务的 Feign Token 转发）
 */
// 注意：common 模块作为纯工具模块，不声明可扫描的 Spring Bean
// 各服务使用自己模块内的 FeignAuthConfig（避免 Bean 冲突和重复扫描）
//@Configuration
public class FeignAuthConfig {

    @Bean
    public RequestInterceptor authHeaderForwardInterceptor() {
        return template -> {
            RequestAttributes attributes = RequestContextHolder.getRequestAttributes();
            if (attributes instanceof ServletRequestAttributes servletAttributes) {
                String authorization = servletAttributes.getRequest().getHeader("Authorization");
                if (authorization != null && !authorization.isBlank()) {
                    template.header("Authorization", authorization);
                }
            }
        };
    }
}
