package org.example.common.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Map;

/**
 * JWT 工具类（统一所有服务的令牌解析实现）
 */
public final class JwtUtil {

    private static final String KEY = "itheima";

    private JwtUtil() {
    }

    /**
     * 解析 JWT Token，返回 claims 载荷
     */
    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(KEY)).build().verify(token).getClaim("claims").asMap();
    }
}
