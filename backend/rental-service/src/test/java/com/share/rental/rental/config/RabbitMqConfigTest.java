package com.share.rental.rental.config;

import com.share.rental.rental.event.PaymentTimeoutMessage;
import org.junit.jupiter.api.Test;
import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageProperties;
import org.springframework.amqp.support.converter.MessageConverter;

import static org.assertj.core.api.Assertions.assertThat;

class RabbitMqConfigTest {

    @Test
    void messageConverterAllowsRentalPaymentTimeoutMessages() {
        RabbitMqConfig config = new RabbitMqConfig();
        MessageConverter converter = config.rabbitMessageConverter();
        PaymentTimeoutMessage source = new PaymentTimeoutMessage("evt-1", 100L);

        Message message = converter.toMessage(source, new MessageProperties());
        Object result = converter.fromMessage(message);

        assertThat(result).isEqualTo(source);
    }
}
