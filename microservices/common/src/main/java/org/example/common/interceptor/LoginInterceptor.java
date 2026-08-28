package org.example.common.interceptor;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.example.common.util.JwtUtil;
import org.example.common.util.ThreadLocalUtil;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.lang.NonNull;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

/**
 * 登录拦截器模板（各服务可继承或自行实现）
 * 注意：没有 @Component，各服务使用自己的拦截器 bean
 * 此文件仅作为统一的参考实现
 */
//@Component
public class LoginInterceptor implements HandlerInterceptor {

    private static final int UNAUTHORIZED = 401;

    private final StringRedisTemplate stringRedisTemplate;

    public LoginInterceptor(StringRedisTemplate stringRedisTemplate) {
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,
                             @NonNull HttpServletResponse response,
                             @NonNull Object handler) throws Exception {
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }
        String token = request.getHeader("Authorization");
        try {
            String redisToken = stringRedisTemplate.opsForValue().get(token);
            if (redisToken == null || !redisToken.equals(token)) {
                response.setStatus(UNAUTHORIZED);
                return false;
            }
            Map<String, Object> claims = JwtUtil.parseToken(token);
            ThreadLocalUtil.set(claims);
            return true;
        } catch (Exception ex) {
            response.setStatus(UNAUTHORIZED);
            return false;
        }
    }

    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,
                                @NonNull HttpServletResponse response,
                                @NonNull Object handler,
                                Exception ex) {
        ThreadLocalUtil.remove();
    }
}
