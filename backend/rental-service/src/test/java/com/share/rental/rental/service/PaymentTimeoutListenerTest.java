package com.share.rental.rental.service;

import com.share.rental.common.redis.RedisKey;
import com.share.rental.rental.event.PaymentTimeoutMessage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.Duration;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class PaymentTimeoutListenerTest {

    @Mock
    private StringRedisTemplate redisTemplate;
    @Mock
    private ValueOperations<String, String> valueOperations;
    @Mock
    private RentalOrderService rentalOrderService;

    private PaymentTimeoutListener listener;

    @BeforeEach
    void setUp() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        listener = new PaymentTimeoutListener(redisTemplate, rentalOrderService);
    }

    @Test
    void handle_firstEventCancelsPendingPaymentOrder() {
        when(valueOperations.setIfAbsent(eq(RedisKey.MQ_IDEMPOTENT + "evt-1"), eq("1"), any(Duration.class)))
                .thenReturn(true);

        listener.handle(new PaymentTimeoutMessage("evt-1", 100L));

        verify(rentalOrderService).cancelPendingPaymentBySystem(100L, "支付超时自动取消");
    }

    @Test
    void handle_duplicateEventDoesNothing() {
        when(valueOperations.setIfAbsent(eq(RedisKey.MQ_IDEMPOTENT + "evt-1"), eq("1"), any(Duration.class)))
                .thenReturn(false);

        listener.handle(new PaymentTimeoutMessage("evt-1", 100L));

        verify(rentalOrderService, never()).cancelPendingPaymentBySystem(any(), any());
    }

    @Test
    void handle_invalidMessageDoesNotTouchRedisOrRequeueByThrowing() {
        listener.handle(new PaymentTimeoutMessage(null, null));

        verify(redisTemplate, never()).opsForValue();
        verify(rentalOrderService, never()).cancelPendingPaymentBySystem(any(), any());
    }
}
