package com.share.rental.rental.service;

import com.share.rental.common.redis.RedisKey;
import com.share.rental.rental.config.RabbitMqConfig;
import com.share.rental.rental.event.PaymentTimeoutMessage;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class PaymentTimeoutListener {

    private static final Duration IDEMPOTENT_TTL = Duration.ofHours(24);

    private final StringRedisTemplate redisTemplate;
    private final RentalOrderService rentalOrderService;

    public PaymentTimeoutListener(StringRedisTemplate redisTemplate, RentalOrderService rentalOrderService) {
        this.redisTemplate = redisTemplate;
        this.rentalOrderService = rentalOrderService;
    }

    @RabbitListener(queues = RabbitMqConfig.PAYMENT_TIMEOUT_DLQ)
    public void handle(PaymentTimeoutMessage message) {
        if (message == null || message.eventId() == null || message.orderId() == null) {
            return;
        }
        String key = RedisKey.MQ_IDEMPOTENT + message.eventId();
        Boolean acquired = redisTemplate.opsForValue().setIfAbsent(key, "1", IDEMPOTENT_TTL);
        if (!Boolean.TRUE.equals(acquired)) {
            return;
        }
        rentalOrderService.cancelPendingPaymentBySystem(message.orderId(), "支付超时自动取消");
    }
}
