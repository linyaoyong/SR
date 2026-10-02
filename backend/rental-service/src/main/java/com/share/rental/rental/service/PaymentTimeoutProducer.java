package com.share.rental.rental.service;

import com.share.rental.rental.config.PaymentTimeoutProperties;
import com.share.rental.rental.config.RabbitMqConfig;
import com.share.rental.rental.event.PaymentTimeoutMessage;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;

import java.util.UUID;

@Service
public class PaymentTimeoutProducer {

    private final RabbitTemplate rabbitTemplate;
    private final PaymentTimeoutProperties properties;

    public PaymentTimeoutProducer(RabbitTemplate rabbitTemplate, PaymentTimeoutProperties properties) {
        this.rabbitTemplate = rabbitTemplate;
        this.properties = properties;
    }

    public void publish(Long orderId) {
        PaymentTimeoutMessage message = new PaymentTimeoutMessage(UUID.randomUUID().toString(), orderId);
        rabbitTemplate.convertAndSend(
                RabbitMqConfig.EXCHANGE,
                RabbitMqConfig.PAYMENT_TIMEOUT_WAIT_ROUTING_KEY,
                message,
                amqpMessage -> {
                    amqpMessage.getMessageProperties().setExpiration(String.valueOf(properties.timeoutMillis()));
                    return amqpMessage;
                });
    }
}
