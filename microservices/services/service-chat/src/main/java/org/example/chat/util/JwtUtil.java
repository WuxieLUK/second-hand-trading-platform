package org.example.chat.util;

import com.auth0.jwt.JWT;
import com.auth0.jwt.algorithms.Algorithm;

import java.util.Map;

public final class JwtUtil {

    private static final String KEY = "itheima";

    private JwtUtil() {
    }

    public static Map<String, Object> parseToken(String token) {
        return JWT.require(Algorithm.HMAC256(KEY)).build().verify(token).getClaim("claims").asMap();
    }
}
