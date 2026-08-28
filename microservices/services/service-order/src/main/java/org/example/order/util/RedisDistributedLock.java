package org.example.order.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class RedisDistributedLock {

    private static final String LOCK_PREFIX = "order:lock:";

    private final RedisTemplate<Object, Object> redisTemplate;

    public RedisDistributedLock(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquireLock(String key) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();
        while (System.currentTimeMillis() - start < 5000) {
            Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, 30, TimeUnit.SECONDS);
            if (Boolean.TRUE.equals(success)) {
                return lockValue;
            }
            try {
                Thread.sleep(50);
            } catch (InterruptedException exception) {
                Thread.currentThread().interrupt();
                log.warn("acquire lock interrupted", exception);
                return null;
            }
        }
        return null;
    }

    public boolean releaseLock(String key, String value) {
        if (value == null) {
            return false;
        }
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";
        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);
        Long result = redisTemplate.execute(redisScript, Collections.singletonList(LOCK_PREFIX + key), value);
        return result != null && result > 0;
    }
}
