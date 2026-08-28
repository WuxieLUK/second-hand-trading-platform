package org.example.product.config;

import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.context.request.RequestAttributes;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

@Configuration
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
