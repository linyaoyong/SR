package com.share.rental.common.redis;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RedisLockSupportTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void runWithLock_acquiredRunsSupplierAndReleasesByOwner() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(true);
        RedisLockSupport support = new RedisLockSupport(redisTemplate);

        String result = support.runWithLock("sr:test:lock", Duration.ofSeconds(10), () -> "ok");

        assertThat(result).isEqualTo("ok");
        verify(valueOperations).setIfAbsent(anyString(), anyString(), any(Duration.class));
        verify(redisTemplate).execute(any(), any(List.class), anyString());
    }

    @Test
    void runWithLock_notAcquiredThrowsTooFrequent() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.setIfAbsent(anyString(), anyString(), any(Duration.class))).thenReturn(false);
        RedisLockSupport support = new RedisLockSupport(redisTemplate);

        assertThatThrownBy(() -> support.runWithLock("sr:test:lock", Duration.ofSeconds(10), () -> "never"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.REQUEST_TOO_FREQUENT));
    }
}
