package org.example.common.util;

import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;

import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis 分布式锁工具类（统一实现）
 * 各服务在自己的 @Configuration 中声明此 Bean，或使用各自现有的实现
 */
//@Component
public class RedisDistributedLock {

    private static final long DEFAULT_TIMEOUT = 30;
    private static final TimeUnit DEFAULT_TIME_UNIT = TimeUnit.SECONDS;
    private final RedisTemplate<Object, Object> redisTemplate;

    public RedisDistributedLock(RedisTemplate<Object, Object> redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public String acquireLock(String key) {
        return acquireLock(key, DEFAULT_TIMEOUT, DEFAULT_TIME_UNIT);
    }

    public String acquireLock(String key, long timeout, TimeUnit unit) {
        String lockKey = "lock:" + key;
        String lockValue = UUID.randomUUID().toString();
        Boolean success = redisTemplate.opsForValue().setIfAbsent(lockKey, lockValue, timeout, unit);
        return Boolean.TRUE.equals(success) ? lockValue : null;
    }

    public void releaseLock(String key, String lockValue) {
        String lockKey = "lock:" + key;
        String currentValue = (String) redisTemplate.opsForValue().get(lockKey);
        if (lockValue.equals(currentValue)) {
            redisTemplate.delete(lockKey);
        }
    }
}
