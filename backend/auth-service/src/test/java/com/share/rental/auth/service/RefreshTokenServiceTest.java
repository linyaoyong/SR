package com.share.rental.auth.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.redis.RedisKey;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RefreshTokenServiceTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;

    @Test
    void issueRefreshToken_writesOpaqueTokenToRedisWithThirtyDayTtl() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        RefreshTokenService service = new RefreshTokenService(redisTemplate);

        String token = service.issueRefreshToken(10L, "alice", "USER");

        assertThat(token).startsWith("rt_user_10_");
        String tokenId = token.substring("rt_user_10_".length());
        verify(valueOperations).set(eq(RedisKey.REFRESH_TOKEN + "10:" + tokenId),
                eq("alice|USER"), eq(Duration.ofDays(30)));
    }

    @Test
    void rotate_validTokenAtomicallyConsumesWithoutIssuingSuccessor() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(RedisKey.REFRESH_TOKEN + "10:old")).thenReturn("alice|USER");
        RefreshTokenService service = new RefreshTokenService(redisTemplate);

        RefreshTokenService.RefreshTokenPayload payload = service.rotate("rt_user_10_old");

        assertThat(payload.userId()).isEqualTo(10L);
        assertThat(payload.username()).isEqualTo("alice");
        assertThat(payload.role()).isEqualTo("USER");
        verify(valueOperations, never()).set(any(String.class), any(String.class), any(Duration.class));
        verify(valueOperations).getAndDelete(RedisKey.REFRESH_TOKEN + "10:old");
        verify(redisTemplate, never()).delete(any(String.class));
    }

    @Test
    void rotate_sameTokenCanOnlyBeConsumedOnce() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        java.util.concurrent.atomic.AtomicReference<String> stored =
                new java.util.concurrent.atomic.AtomicReference<>("alice|USER");
        lenient().when(valueOperations.getAndDelete(RedisKey.REFRESH_TOKEN + "10:old"))
                .thenAnswer(invocation -> stored.getAndSet(null));
        lenient().when(valueOperations.get(RedisKey.REFRESH_TOKEN + "10:old"))
                .thenAnswer(invocation -> stored.get());
        lenient().when(redisTemplate.delete(RedisKey.REFRESH_TOKEN + "10:old"))
                .thenAnswer(invocation -> stored.getAndSet(null) != null);
        RefreshTokenService service = new RefreshTokenService(redisTemplate);
        service.rotate("rt_user_10_old");
        assertThatThrownBy(() -> service.rotate("rt_user_10_old"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }

    @Test
    void rotate_concurrentRequestsHaveExactlyOneWinner() throws Exception {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        java.util.concurrent.atomic.AtomicReference<String> stored =
                new java.util.concurrent.atomic.AtomicReference<>("alice|USER");
        java.util.concurrent.CountDownLatch bothLegacyReads = new java.util.concurrent.CountDownLatch(2);
        lenient().when(valueOperations.get(RedisKey.REFRESH_TOKEN + "10:old")).thenAnswer(invocation -> {
            String value = stored.get();
            bothLegacyReads.countDown();
            if (!bothLegacyReads.await(5, java.util.concurrent.TimeUnit.SECONDS)) {
                throw new AssertionError("Both legacy readers must reach the race barrier");
            }
            return value;
        });
        lenient().when(redisTemplate.delete(RedisKey.REFRESH_TOKEN + "10:old"))
                .thenAnswer(invocation -> stored.getAndSet(null) != null);
        lenient().when(valueOperations.getAndDelete(RedisKey.REFRESH_TOKEN + "10:old"))
                .thenAnswer(invocation -> stored.getAndSet(null));
        RefreshTokenService service = new RefreshTokenService(redisTemplate);
        java.util.concurrent.ExecutorService executor = java.util.concurrent.Executors.newFixedThreadPool(2);
        try {
            java.util.concurrent.Callable<Boolean> attempt = () -> {
                try {
                    service.rotate("rt_user_10_old");
                    return true;
                } catch (BusinessException ex) {
                    assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID);
                    return false;
                }
            };
            var first = executor.submit(attempt);
            var second = executor.submit(attempt);
            int winners = (first.get(10, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0)
                    + (second.get(10, java.util.concurrent.TimeUnit.SECONDS) ? 1 : 0);
            assertThat(winners).isEqualTo(1);
        } finally {
            executor.shutdownNow();
        }
    }

    @Test
    void rotate_missingRedisKeyRejectsToken() {
        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(valueOperations.getAndDelete(RedisKey.REFRESH_TOKEN + "10:old")).thenReturn(null);
        RefreshTokenService service = new RefreshTokenService(redisTemplate);

        assertThatThrownBy(() -> service.rotate("rt_user_10_old"))
                .isInstanceOfSatisfying(BusinessException.class,
                        ex -> assertThat(ex.errorCode()).isEqualTo(ErrorCode.AUTH_REFRESH_TOKEN_INVALID));
    }
}
