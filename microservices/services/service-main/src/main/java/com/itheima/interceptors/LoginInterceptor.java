package com.itheima.interceptors;

import com.itheima.util.JwtUtil;
import com.itheima.util.ThreadLocalUtil;
import io.micrometer.common.lang.NonNull;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.util.Map;

//登录拦截器配置,类似于过渡区域
@Component
public class LoginInterceptor implements HandlerInterceptor {

    @Autowired
    private StringRedisTemplate stringRedisTemplate;
    @Override
    public boolean preHandle(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull Object handler) {

        // 处理OPTIONS请求，直接放行
        if ("OPTIONS".equalsIgnoreCase(request.getMethod())) {
            // 设置CORS响应头
            response.setHeader("Access-Control-Allow-Origin", "http://localhost:5173");
            response.setHeader("Access-Control-Allow-Methods", "GET, POST, PUT, DELETE, OPTIONS");
            response.setHeader("Access-Control-Allow-Headers", "*");
            response.setHeader("Access-Control-Allow-Credentials", "true");
            response.setStatus(HttpServletResponse.SC_OK);
            return true;
        }


        //从浏览器获得的令牌验证
        String token = request.getHeader("Authorization");
        try {
            //从redis中获取相同的令牌token
            ValueOperations<String, String> operations= stringRedisTemplate.opsForValue();
            String redisToken = operations.get(token);
            if (redisToken==null){
                //token已经失效，抛出异常，跳到catch
               throw new RuntimeException();
            }
            if( !token.equals(redisToken)){
                //令牌已经失效，抛出异常，跳到catch
                throw new RuntimeException();
            }
            //验证令牌
            Map<String, Object> claims = JwtUtil.parseToken(token);
            //把业务数据存储到ThreadLocal中，即令牌信息
            ThreadLocalUtil.set(claims);
            //放行
            return true;
        } catch (Exception e) {
            //http响应状态码为401
            //token过期或篡改
            response.setStatus(401);
            //不放行
            return false;
        }

    }
    @Override
    public void afterCompletion(@NonNull HttpServletRequest request,@NonNull HttpServletResponse response,@NonNull Object handler, Exception ex) throws Exception {
        //请求结束，清理ThreadLocal中的数据，防止内存泄露
        ThreadLocalUtil.remove();
    }
}
