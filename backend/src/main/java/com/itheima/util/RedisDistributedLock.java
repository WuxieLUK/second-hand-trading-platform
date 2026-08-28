package com.itheima.util;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.data.redis.core.script.DefaultRedisScript;
import org.springframework.stereotype.Component;

import java.util.Collections;
import java.util.UUID;
import java.util.concurrent.TimeUnit;

/**
 * Redis分布式锁工具类
 */
@Slf4j
@Component
public class RedisDistributedLock {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    // 锁的前缀
    private static final String LOCK_PREFIX = "order:lock:";
    // 默认锁超时时间（秒），防止死锁
    private static final long DEFAULT_LOCK_TIMEOUT = 30;
    // 默认获取锁超时时间（毫秒）
    private static final long DEFAULT_ACQUIRE_TIMEOUT = 5000;

    /**
     * 获取分布式锁
     * @param key 锁的key（如商品ID）
     * @return 锁标识（用于释放锁），获取失败返回null
     */
    public String acquireLock(String key) {
        return acquireLock(key, DEFAULT_LOCK_TIMEOUT, DEFAULT_ACQUIRE_TIMEOUT);
    }

    /**
     * 获取分布式锁（自定义超时）
     * @param key 锁的key
     * @param lockTimeout 锁超时时间（秒）
     * @param acquireTimeout 获取锁超时时间（毫秒）
     * @return 锁标识
     */
    public String acquireLock(String key, long lockTimeout, long acquireTimeout) {
        String lockKey = LOCK_PREFIX + key;
        String lockValue = UUID.randomUUID().toString();
        long start = System.currentTimeMillis();

        try {
            while (System.currentTimeMillis() - start < acquireTimeout) {
                // SET NX EX 原子操作：不存在则设置，设置过期时间
                Boolean success = redisTemplate.opsForValue()
                        .setIfAbsent(lockKey, lockValue, lockTimeout, TimeUnit.SECONDS);

                if (Boolean.TRUE.equals(success)) {
                    log.info("获取分布式锁成功，key：{}，value：{}", lockKey, lockValue);
                    return lockValue;
                }

                // 短暂休眠，避免自旋太频繁
                Thread.sleep(50);
            }
        } catch (InterruptedException e) {
            log.error("获取分布式锁被中断", e);
            Thread.currentThread().interrupt();
        }

        log.warn("获取分布式锁超时，key：{}", lockKey);
        return null;
    }

    /**
     * 释放分布式锁（Lua脚本保证原子性）
     * @param key 锁的key
     * @param value 锁标识
     * @return 是否释放成功
     */
    public boolean releaseLock(String key, String value) {
        if (value == null) {
            return false;
        }

        String lockKey = LOCK_PREFIX + key;
        // Lua脚本：先判断是否是自己的锁，再删除
        String script = "if redis.call('get', KEYS[1]) == ARGV[1] then return redis.call('del', KEYS[1]) else return 0 end";

        DefaultRedisScript<Long> redisScript = new DefaultRedisScript<>();
        redisScript.setScriptText(script);
        redisScript.setResultType(Long.class);

        Long result = redisTemplate.execute(redisScript, Collections.singletonList(lockKey), value);

        boolean success = result != null && result > 0;
        if (success) {
            log.info("释放分布式锁成功，key：{}", lockKey);
        } else {
            log.warn("释放分布式锁失败，key：{}，可能锁已过期或不是当前线程持有", lockKey);
        }
        return success;
    }
}