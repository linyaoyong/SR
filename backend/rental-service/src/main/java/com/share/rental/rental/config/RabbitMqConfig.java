package com.share.rental.rental.config;

import org.springframework.amqp.core.Binding;
import org.springframework.amqp.core.BindingBuilder;
import org.springframework.amqp.core.DirectExchange;
import org.springframework.amqp.core.Queue;
import org.springframework.amqp.core.QueueBuilder;
import org.springframework.amqp.support.converter.MessageConverter;
import org.springframework.amqp.support.converter.SimpleMessageConverter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.List;

@Configuration
public class RabbitMqConfig {

    public static final String EXCHANGE = "sr.rental.exchange";
    public static final String PAYMENT_TIMEOUT_WAIT_QUEUE = "sr.rental.payment.timeout.wait";
    public static final String PAYMENT_TIMEOUT_DLQ = "sr.rental.payment.timeout.dlq";
    public static final String PAYMENT_TIMEOUT_WAIT_ROUTING_KEY = "rental.payment.timeout.wait";
    public static final String PAYMENT_TIMEOUT_DEAD_ROUTING_KEY = "rental.payment.timeout.dead";

    @Bean
    public DirectExchange rentalExchange() {
        return new DirectExchange(EXCHANGE, true, false);
    }

    @Bean
    public Queue paymentTimeoutWaitQueue() {
        return QueueBuilder.durable(PAYMENT_TIMEOUT_WAIT_QUEUE)
                .deadLetterExchange(EXCHANGE)
                .deadLetterRoutingKey(PAYMENT_TIMEOUT_DEAD_ROUTING_KEY)
                .build();
    }

    @Bean
    public Queue paymentTimeoutDlq() {
        return QueueBuilder.durable(PAYMENT_TIMEOUT_DLQ).build();
    }

    @Bean
    public Binding paymentTimeoutWaitBinding(DirectExchange rentalExchange, Queue paymentTimeoutWaitQueue) {
        return BindingBuilder.bind(paymentTimeoutWaitQueue)
                .to(rentalExchange)
                .with(PAYMENT_TIMEOUT_WAIT_ROUTING_KEY);
    }

    @Bean
    public Binding paymentTimeoutDlqBinding(DirectExchange rentalExchange, Queue paymentTimeoutDlq) {
        return BindingBuilder.bind(paymentTimeoutDlq)
                .to(rentalExchange)
                .with(PAYMENT_TIMEOUT_DEAD_ROUTING_KEY);
    }

    @Bean
    public MessageConverter rabbitMessageConverter() {
        SimpleMessageConverter converter = new SimpleMessageConverter();
        converter.setAllowedListPatterns(List.of("com.share.rental.rental.event.*"));
        return converter;
    }
}
