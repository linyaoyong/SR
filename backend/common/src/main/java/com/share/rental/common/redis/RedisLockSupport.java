package com.share.rental.common.redis;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.script.RedisScript;

import java.time.Duration;
import java.util.List;
import java.util.UUID;
import java.util.function.Supplier;

public class RedisLockSupport {

    private static final RedisScript<Long> RELEASE_SCRIPT = RedisScript.of("""
            if redis.call('get', KEYS[1]) == ARGV[1] then
                return redis.call('del', KEYS[1])
            end
            return 0
            """, Long.class);

    private final StringRedisTemplate redisTemplate;

    public RedisLockSupport(StringRedisTemplate redisTemplate) {
        this.redisTemplate = redisTemplate;
    }

    public <T> T runWithLock(String key, Duration ttl, Supplier<T> supplier) {
        String owner = UUID.randomUUID().toString();
        Boolean locked = redisTemplate.opsForValue().setIfAbsent(key, owner, ttl);
        if (!Boolean.TRUE.equals(locked)) {
            throw new BusinessException(ErrorCode.REQUEST_TOO_FREQUENT);
        }
        try {
            return supplier.get();
        } finally {
            redisTemplate.execute(RELEASE_SCRIPT, List.of(key), owner);
        }
    }
}
