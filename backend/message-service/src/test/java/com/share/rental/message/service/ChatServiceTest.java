package com.share.rental.message.service;

import com.share.rental.common.upload.ImageUploadService;
import com.share.rental.common.upload.UploadedFile;
import com.share.rental.message.dto.CardMessageRequest;
import com.share.rental.message.dto.SendMessageRequest;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.message.client.AuthMessageClient;
import com.share.rental.message.client.ItemMessageClient;
import com.share.rental.message.dto.ConversationResponse;
import com.share.rental.message.dto.ItemSummary;
import com.share.rental.message.dto.UserPublicSummary;
import com.share.rental.message.entity.Conversation;
import com.share.rental.message.entity.Message;
import com.share.rental.message.mapper.ConversationMapper;
import com.share.rental.message.mapper.MessageMapper;
import com.share.rental.message.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.util.Arrays;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatServiceTest {

    @Mock
    ConversationMapper conversationMapper;
    @Mock
    MessageMapper messageMapper;
    @Mock
    ChatWebSocketHandler webSocketHandler;
    @Mock
    ObjectMapper objectMapper;
    @Mock
    StringRedisTemplate redisTemplate;
    @Mock
    ValueOperations<String, String> valueOperations;
    @Mock
    AuthMessageClient authMessageClient;
    @Mock
    ItemMessageClient itemMessageClient;
    @Mock
    ImageUploadService imageUploadService;
    @InjectMocks
    ChatService chatService;

    @org.junit.jupiter.api.BeforeEach
    void setUpRedis() {
        lenient().when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        lenient().when(conversationMapper.selectList(any())).thenReturn(List.of());
    }

    @Test
    void listConversations_returnsUserConversationsWithUnreadCount() {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setItemId(10L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        conv.setLastMessageContent("hello");
        conv.setUserAUnreadCount(2);
        conv.setUserBUnreadCount(0);
        when(conversationMapper.selectList(any())).thenReturn(Arrays.asList(conv));
        when(itemMessageClient.getInfo(10L)).thenReturn(
                ApiResponse.success(new ItemSummary(10L, 100L, "电钻", "http://img/1.jpg")));
        when(authMessageClient.getPublic(200L)).thenReturn(
                ApiResponse.success(new UserPublicSummary(200L, "张三", "http://avatar/200.jpg", 1, 80)));

        List<?> result = chatService.listConversations(100L);

        assertThat(result).hasSize(1);
        ConversationResponse resp = (ConversationResponse) result.get(0);
        assertThat(resp.getUnreadCount()).isEqualTo(2);
        assertThat(resp.getItemTitle()).isEqualTo("电钻");
        assertThat(resp.getItemFirstImageUrl()).isEqualTo("http://img/1.jpg");
        assertThat(resp.getPeerUserId()).isEqualTo(200L);
        assertThat(resp.getPeerUsername()).isEqualTo("张三");
        assertThat(resp.getPeerAvatarUrl()).isEqualTo("http://avatar/200.jpg");
    }

    @Test
    void listConversations_fallsBackWhenRemoteSummaryFails() {
        Conversation conv = new Conversation();
        conv.setId(2L);
        conv.setItemId(10L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        conv.setLastMessageContent("hi");
        conv.setUserAUnreadCount(0);
        conv.setUserBUnreadCount(0);
        when(conversationMapper.selectList(any())).thenReturn(Arrays.asList(conv));
        when(itemMessageClient.getInfo(10L)).thenThrow(new RuntimeException("item down"));
        when(authMessageClient.getPublic(200L)).thenThrow(new RuntimeException("auth down"));

        List<?> result = chatService.listConversations(100L);

        assertThat(result).hasSize(1);
        ConversationResponse resp = (ConversationResponse) result.get(0);
        assertThat(resp.getItemTitle()).isEqualTo("物品 #10");
        assertThat(resp.getItemFirstImageUrl()).isNull();
        assertThat(resp.getPeerUserId()).isEqualTo(200L);
        assertThat(resp.getPeerUsername()).isEqualTo("用户 #200");
        assertThat(resp.getPeerAvatarUrl()).isNull();
    }

    @Test
    void systemConversationUsesSystemSummary() {
        Conversation conv = new Conversation();
        conv.setId(3L);
        conv.setItemId(0L);
        conv.setUserAId(100L);
        conv.setUserBId(0L);
        conv.setLastMessageContent("系统通知");
        conv.setUserAUnreadCount(0);
        conv.setUserBUnreadCount(0);
        when(conversationMapper.selectList(any())).thenReturn(Arrays.asList(conv));

        List<?> result = chatService.listConversations(100L);

        assertThat(result).hasSize(1);
        ConversationResponse resp = (ConversationResponse) result.get(0);
        assertThat(resp.getItemTitle()).isNull();
        assertThat(resp.getItemFirstImageUrl()).isNull();
        assertThat(resp.getPeerUserId()).isEqualTo(0L);
        assertThat(resp.getPeerUsername()).isEqualTo("系统通知");
        assertThat(resp.getPeerAvatarUrl()).isNull();
        verifyNoInteractions(itemMessageClient);
        verifyNoInteractions(authMessageClient);
    }

    @Test
    void openConversation_reusesExistingConversationForItemAndUsers() {
        Conversation existing = new Conversation();
        existing.setId(9L);
        existing.setItemId(88L);
        existing.setUserAId(100L);
        existing.setUserBId(200L);
        when(conversationMapper.selectOne(any())).thenReturn(existing);

        var response = chatService.openConversation(100L, 200L, 88L);

        assertThat(response.getId()).isEqualTo(9L);
        verify(conversationMapper, never()).insert(any(Conversation.class));
    }

    @Test
    void openConversation_createsConversationWithStableUserOrder() {
        when(conversationMapper.selectOne(any())).thenReturn(null);

        var response = chatService.openConversation(200L, 100L, 88L);

        ArgumentCaptor<Conversation> captor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).insert(captor.capture());
        Conversation created = captor.getValue();
        assertThat(created.getItemId()).isEqualTo(88L);
        assertThat(created.getUserAId()).isEqualTo(100L);
        assertThat(created.getUserBId()).isEqualTo(200L);
        assertThat(created.getUserAUnreadCount()).isZero();
        assertThat(created.getUserBUnreadCount()).isZero();
        assertThat(response.getItemId()).isEqualTo(88L);
    }

    @Test
    void createCardMessage_reusesConversationAndSendsMessage() {
        // selectOne 返回 null 模拟新建会话；insert 时回填 id
        when(conversationMapper.selectOne(any())).thenReturn(null);
        when(conversationMapper.insert(any(Conversation.class))).thenAnswer(inv -> {
            Conversation c = inv.getArgument(0);
            c.setId(99L);
            return 1;
        });

        // selectById 返回有效会话，让 sendMessage 通过
        Conversation conv = new Conversation();
        conv.setId(99L);
        conv.setItemId(50L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        conv.setUserAUnreadCount(0);
        conv.setUserBUnreadCount(0);
        when(conversationMapper.selectById(99L)).thenReturn(conv);

        CardMessageRequest request = new CardMessageRequest();
        request.setSenderId(100L);
        request.setReceiverId(200L);
        request.setItemId(50L);
        request.setContent("收到新的租借申请：电钻");
        request.setCardType(1);
        request.setRelatedApplicationId(500L);

        chatService.createCardMessage(request);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(msgCaptor.capture());
        Message msg = msgCaptor.getValue();
        assertThat(msg.getMessageType()).isEqualTo(3);
        assertThat(msg.getCardType()).isEqualTo(1);
        assertThat(msg.getContent()).isEqualTo("收到新的租借申请：电钻");
        assertThat(msg.getRelatedApplicationId()).isEqualTo(500L);

        verify(conversationMapper).updateById(any(Conversation.class));
    }

    @Test
    void listMessages_returnsMessagesForConversation() {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        when(conversationMapper.selectById(1L)).thenReturn(conv);

        Message msg = new Message();
        msg.setId(1L);
        msg.setConversationId(1L);
        msg.setSenderId(100L);
        msg.setContent("test");
        msg.setMessageType(1);
        when(messageMapper.selectList(any())).thenReturn(Arrays.asList(msg));

        var result = chatService.listMessages(1L, 100L, 1, 20);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getContent()).isEqualTo("test");
    }

    @Test
    void listMessages_nonParticipant_throws() {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        when(conversationMapper.selectById(1L)).thenReturn(conv);

        assertThatThrownBy(() -> chatService.listMessages(1L, 300L, 1, 20))
                .isInstanceOf(BusinessException.class);

        verify(messageMapper, never()).selectList(any());
    }

    @Test
    void sendMessage_createsMessageAndUpdatesConversation() {
        SendMessageRequest request = new SendMessageRequest();
        request.setMessageType(1);
        request.setContent("hello world");

        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        conv.setUserAUnreadCount(0);
        conv.setUserBUnreadCount(0);
        when(conversationMapper.selectById(1L)).thenReturn(conv);

        chatService.sendMessage(1L, 100L, request);

        ArgumentCaptor<Message> msgCaptor = ArgumentCaptor.forClass(Message.class);
        verify(messageMapper).insert(msgCaptor.capture());
        Message msg = msgCaptor.getValue();
        assertThat(msg.getSenderId()).isEqualTo(100L);
        assertThat(msg.getReceiverId()).isEqualTo(200L);
        assertThat(msg.getContent()).isEqualTo("hello world");

        ArgumentCaptor<Conversation> convCaptor = ArgumentCaptor.forClass(Conversation.class);
        verify(conversationMapper).updateById(convCaptor.capture());
        assertThat(convCaptor.getValue().getUserBUnreadCount()).isEqualTo(1);
        assertThat(convCaptor.getValue().getLastMessageContent()).isEqualTo("hello world");
        verify(valueOperations).increment("sr:message:unread:200");
    }

    @Test
    void sendMessage_nonParticipant_throws() {
        SendMessageRequest request = new SendMessageRequest();
        request.setMessageType(1);
        request.setContent("hello world");

        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        when(conversationMapper.selectById(1L)).thenReturn(conv);

        assertThatThrownBy(() -> chatService.sendMessage(1L, 300L, request))
                .isInstanceOf(BusinessException.class);

        verify(messageMapper, never()).insert(any(Message.class));
        verify(conversationMapper, never()).updateById(any(Conversation.class));
    }

    @Test
    void markAsRead_clearsUnreadCount() {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        conv.setUserAUnreadCount(3);
        conv.setUserBUnreadCount(0);
        when(conversationMapper.selectById(1L)).thenReturn(conv);

        chatService.markAsRead(1L, 100L);

        assertThat(conv.getUserAUnreadCount()).isEqualTo(0);
        verify(conversationMapper).updateById(conv);
        verify(messageMapper).update(any(), any());
        verify(redisTemplate).delete("sr:message:unread:100");
    }

    @Test
    void markAsRead_pushesReadReceiptToOtherParticipant() throws Exception {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        conv.setUserAUnreadCount(1);
        conv.setUserBUnreadCount(0);
        when(conversationMapper.selectById(1L)).thenReturn(conv);
        when(objectMapper.writeValueAsString(any())).thenReturn(
                "{\"type\":\"READ_RECEIPT\",\"conversationId\":1,\"readerId\":100}");

        chatService.markAsRead(1L, 100L);

        verify(webSocketHandler).sendToUser(eq(200L), contains("\"READ_RECEIPT\""));
    }

    @Test
    void markAsRead_nonParticipant_throws() {
        Conversation conv = new Conversation();
        conv.setId(1L);
        conv.setUserAId(100L);
        conv.setUserBId(200L);
        when(conversationMapper.selectById(1L)).thenReturn(conv);

        assertThatThrownBy(() -> chatService.markAsRead(1L, 300L))
                .isInstanceOf(BusinessException.class);

        verify(conversationMapper, never()).updateById(any(Conversation.class));
        verify(messageMapper, never()).update(any(), any());
    }

    @Test
    void getUnreadCount_sumsBothSides() {
        Conversation c1 = new Conversation();
        c1.setUserAId(100L);
        c1.setUserAUnreadCount(2);
        c1.setUserBUnreadCount(0);
        Conversation c2 = new Conversation();
        c2.setUserAId(50L);
        c2.setUserBId(100L);
        c2.setUserAUnreadCount(0);
        c2.setUserBUnreadCount(3);
        when(conversationMapper.selectList(any())).thenReturn(Arrays.asList(c1, c2));

        var result = chatService.getUnreadCount(100L);

        assertThat(result.getTotalUnread()).isEqualTo(5);
        verify(valueOperations).set("sr:message:unread:100", "5");
    }

    @Test
    void getUnreadCount_cacheHitAvoidsDbQuery() {
        when(valueOperations.get("sr:message:unread:100")).thenReturn("7");

        var result = chatService.getUnreadCount(100L);

        assertThat(result.getTotalUnread()).isEqualTo(7);
        verify(conversationMapper, never()).selectList(any());
    }

    @Test
    void uploadMessageImage_delegatesToImageUploadServiceWithMessagesBucket() {
        org.springframework.web.multipart.MultipartFile file =
                org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        org.mockito.Mockito.lenient().when(file.isEmpty()).thenReturn(false);
        UploadedFile uploaded = new UploadedFile("/files/messages/abc.jpg", "abc.jpg", "image/jpeg", 1024);
        when(imageUploadService.storeImage(file, "messages")).thenReturn(uploaded);

        UploadedFile result = chatService.uploadMessageImage(100L, file);

        assertThat(result.url()).isEqualTo("/files/messages/abc.jpg");
        verify(imageUploadService).storeImage(file, "messages");
    }

    @Test
    void uploadMessageImage_rejectsEmptyFile() {
        org.springframework.web.multipart.MultipartFile file =
                org.mockito.Mockito.mock(org.springframework.web.multipart.MultipartFile.class);
        when(file.isEmpty()).thenReturn(true);

        assertThatThrownBy(() -> chatService.uploadMessageImage(100L, file))
                .isInstanceOf(BusinessException.class);
        verify(imageUploadService, never()).storeImage(any(), anyString());
    }
}
