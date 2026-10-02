package com.share.rental.message.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.message.dto.ConversationResponse;
import com.share.rental.message.dto.MessageResponse;
import com.share.rental.message.dto.SendMessageRequest;
import com.share.rental.message.dto.UnreadCountResponse;
import com.share.rental.message.service.ChatService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.Collections;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(MessageController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class MessageControllerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.web.context.WebApplicationContext gatewayFixtureContext;

    @org.junit.jupiter.api.BeforeEach
    void useExplicitGatewayCredentialFixture() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(gatewayFixtureContext)
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .header("X-Internal-Token", "test-only-backend-ingress-token"))
                .build();
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ChatService chatService;

    @Test
    void listConversations_returns200() throws Exception {
        when(chatService.listConversations(eq(100L))).thenReturn(Collections.singletonList(
                new ConversationResponse(1L, 10L, 100L, 200L, "hello", LocalDateTime.now(), 2,
                        null, null, null, null, null)));

        mvc.perform(get("/api/messages/conversations").header("X-User-Id", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].unreadCount").value(2));
    }

    @Test
    void listMessages_returns200() throws Exception {
        when(chatService.listMessages(eq(1L), eq(100L), eq(1), eq(20))).thenReturn(
                Collections.singletonList(new MessageResponse(
                        1L, 1L, 100L, 200L, 1, null, "test", null, null, null, 0, LocalDateTime.now())));

        mvc.perform(get("/api/messages/conversations/1/messages")
                        .header("X-User-Id", 100)
                        .param("page", "1")
                        .param("size", "20"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].content").value("test"));
    }

    @Test
    void sendMessage_returns200() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setMessageType(1);
        req.setContent("hello");
        when(chatService.sendMessage(eq(1L), eq(100L), any())).thenReturn(new MessageResponse(
                1L, 1L, 100L, 200L, 1, null, "hello", null, null, null, 0, LocalDateTime.now()));

        mvc.perform(post("/api/messages/conversations/1/messages")
                        .header("X-User-Id", 100)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.content").value("hello"));
    }

    @Test
    void markAsRead_returns200() throws Exception {
        doNothing().when(chatService).markAsRead(eq(1L), eq(100L));

        mvc.perform(put("/api/messages/conversations/1/read").header("X-User-Id", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0));
    }

    @Test
    void getUnreadCount_returns200() throws Exception {
        when(chatService.getUnreadCount(eq(100L))).thenReturn(new UnreadCountResponse(3));

        mvc.perform(get("/api/messages/unread-count").header("X-User-Id", 100))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.totalUnread").value(3));
    }

    @Test
    void sendMessage_missingType_returns400() throws Exception {
        SendMessageRequest req = new SendMessageRequest();
        req.setContent("hello"); // messageType missing -> @NotNull violation

        mvc.perform(post("/api/messages/conversations/1/messages")
                        .header("X-User-Id", 100)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }
}
