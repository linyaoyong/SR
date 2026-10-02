package com.share.rental.common.redis;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class RedisKeyTest {

    @Test
    void allStage8KeysUseProjectPrefix() {
        List<String> keys = List.of(
                RedisKey.REFRESH_TOKEN,
                RedisKey.USER_BLACKLIST,
                RedisKey.ITEM_CACHE,
                RedisKey.APPLICATION_DUPLICATE,
                RedisKey.TIME_LOCK,
                RedisKey.MESSAGE_UNREAD,
                RedisKey.WEBSOCKET_ONLINE,
                RedisKey.MQ_IDEMPOTENT,
                RedisKey.SCHEDULER_LOCK
        );

        assertThat(keys)
                .allSatisfy(key -> assertThat(key).startsWith("sr:"));
    }
}
