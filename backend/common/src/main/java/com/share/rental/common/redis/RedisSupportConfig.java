package com.share.rental.common.redis;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.redis.core.StringRedisTemplate;

@Configuration(proxyBeanMethods = false)
@ConditionalOnClass(name = "org.springframework.data.redis.core.StringRedisTemplate")
public class RedisSupportConfig {

    @Bean
    public RedisLockSupport redisLockSupport(StringRedisTemplate redisTemplate) {
        return new RedisLockSupport(redisTemplate);
    }
}
