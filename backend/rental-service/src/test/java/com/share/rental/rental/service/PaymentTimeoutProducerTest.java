package com.share.rental.rental.service;

import com.share.rental.rental.config.PaymentTimeoutProperties;
import com.share.rental.rental.config.RabbitMqConfig;
import com.share.rental.rental.event.PaymentTimeoutMessage;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.amqp.core.MessagePostProcessor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class PaymentTimeoutProducerTest {

    @Test
    void publish_usesDemoTimeoutSecondsWhenConfigured() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        PaymentTimeoutProperties properties = new PaymentTimeoutProperties();
        properties.setTimeoutMinutes(30);
        properties.setDemoTimeoutSeconds(30L);
        PaymentTimeoutProducer producer = new PaymentTimeoutProducer(rabbitTemplate, properties);

        producer.publish(100L);

        ArgumentCaptor<PaymentTimeoutMessage> messageCaptor = ArgumentCaptor.forClass(PaymentTimeoutMessage.class);
        ArgumentCaptor<MessagePostProcessor> processorCaptor = ArgumentCaptor.forClass(MessagePostProcessor.class);
        verify(rabbitTemplate).convertAndSend(eq(RabbitMqConfig.EXCHANGE), eq(RabbitMqConfig.PAYMENT_TIMEOUT_WAIT_ROUTING_KEY),
                messageCaptor.capture(), processorCaptor.capture());
        assertThat(messageCaptor.getValue().orderId()).isEqualTo(100L);
        org.springframework.amqp.core.Message message = new org.springframework.amqp.core.Message(new byte[0]);
        processorCaptor.getValue().postProcessMessage(message);
        assertThat(message.getMessageProperties().getExpiration()).isEqualTo("30000");
    }
}
