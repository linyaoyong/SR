package com.share.rental.message;

import com.share.rental.message.entity.Message;
import com.share.rental.message.enums.CardTypeEnum;
import com.share.rental.message.enums.MessageTypeEnum;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class MessageEntityCompileTest {

    @Test
    void enumsExposeExpectedCodes() {
        assertThat(MessageTypeEnum.TEXT.code()).isEqualTo(1);
        assertThat(MessageTypeEnum.AUDIT_NOTIFICATION.code()).isEqualTo(5);
        assertThat(CardTypeEnum.APPLICATION.code()).isEqualTo(1);
        assertThat(CardTypeEnum.DISPUTE.code()).isEqualTo(5);
    }

    @Test
    void entitiesCanInstantiate() {
        Message message = new Message();
        message.setContent("你好");
        message.setMessageType(MessageTypeEnum.TEXT.code());
        assertThat(message.getContent()).isEqualTo("你好");
        assertThat(message.getMessageType()).isEqualTo(1);
    }
}
