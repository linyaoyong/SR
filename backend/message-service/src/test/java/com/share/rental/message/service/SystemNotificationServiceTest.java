package com.share.rental.message.service;

import com.share.rental.message.dto.SystemNotificationRequest;
import com.share.rental.message.entity.Conversation;
import com.share.rental.message.entity.Message;
import com.share.rental.message.mapper.ConversationMapper;
import com.share.rental.message.mapper.MessageMapper;
import com.share.rental.message.websocket.ChatWebSocketHandler;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SystemNotificationServiceTest {

    @Mock
    private ConversationMapper conversationMapper;

    @Mock
    private MessageMapper messageMapper;

    @Mock
    private ChatWebSocketHandler webSocketHandler;

    @InjectMocks
    private SystemNotificationService systemNotificationService;

    @Test
    void createAuditNotification_createsSystemConversationAndUnreadMessage() {
        SystemNotificationRequest request = new SystemNotificationRequest(10L, "物品需要整改", 5);
        when(conversationMapper.selectOne(any())).thenReturn(null);

        systemNotificationService.createSystemNotification(request);

        verify(conversationMapper).insert(ArgumentMatchers.<Conversation>argThat(
                c -> c.getItemId().equals(0L) && c.getUserBId().equals(10L)));
        verify(messageMapper).insert(ArgumentMatchers.<Message>argThat(
                m -> m.getReceiverId().equals(10L)
                        && m.getMessageType().equals(5)
                        && m.getIsRead().equals(0)));
    }
}
