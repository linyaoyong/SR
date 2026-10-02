package com.share.rental.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.message.dto.SystemNotificationRequest;
import com.share.rental.message.entity.Conversation;
import com.share.rental.message.entity.Message;
import com.share.rental.message.mapper.ConversationMapper;
import com.share.rental.message.mapper.MessageMapper;
import com.share.rental.message.websocket.ChatWebSocketHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
public class SystemNotificationService {

    private static final Logger log = LoggerFactory.getLogger(SystemNotificationService.class);
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ChatWebSocketHandler webSocketHandler;

    public SystemNotificationService(ConversationMapper conversationMapper, MessageMapper messageMapper,
                                    ChatWebSocketHandler webSocketHandler) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.webSocketHandler = webSocketHandler;
    }

    public void createSystemNotification(SystemNotificationRequest request) {
        Long receiverId = request.getReceiverId();

        // 1. Find or create system conversation: item_id=0, user_a_id=0, user_b_id=receiverId
        Conversation conversation = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getItemId, 0L)
                        .eq(Conversation::getUserAId, 0L)
                        .eq(Conversation::getUserBId, receiverId));

        boolean isNew = conversation == null;
        if (isNew) {
            conversation = new Conversation();
            conversation.setItemId(0L);
            conversation.setUserAId(0L);
            conversation.setUserBId(receiverId);
            conversation.setUserAUnreadCount(0);
            conversation.setUserBUnreadCount(0);
        }

        // 2. Increment user_b_unread_count
        int currentUnread = conversation.getUserBUnreadCount() == null ? 0 : conversation.getUserBUnreadCount();
        conversation.setUserBUnreadCount(currentUnread + 1);

        // 3. Update lastMessageContent + lastMessageTime
        conversation.setLastMessageContent(request.getContent());
        conversation.setLastMessageTime(LocalDateTime.now());

        if (isNew) {
            conversationMapper.insert(conversation);
        } else {
            conversationMapper.updateById(conversation);
        }

        // 4. Insert Message
        Message message = new Message();
        message.setConversationId(conversation.getId());
        message.setSenderId(0L);
        message.setReceiverId(receiverId);
        message.setMessageType(request.getMessageType());
        message.setContent(request.getContent());
        message.setIsRead(0);
        messageMapper.insert(message);

        // Push via WebSocket if receiver is online
        try {
            webSocketHandler.sendToUser(receiverId, request.getContent());
        } catch (Exception e) {
            // WebSocket push failure should not block notification persistence
            log.warn("WebSocket push failed for system notification receiverId={}: {}", receiverId, e.getMessage());
        }
    }
}
