package com.share.rental.message.websocket;

import com.share.rental.common.redis.RedisKey;
import com.share.rental.common.security.JwtClaims;
import com.share.rental.common.security.JwtUtil;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.net.URI;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatWebSocketHandlerTest {

    @Test
    void sendToUser_serializesConcurrentWritesToSameSession() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        WebSocketSession session = mock(WebSocketSession.class);
        AtomicBoolean writing = new AtomicBoolean(false);
        AtomicInteger concurrentWrites = new AtomicInteger(0);
        CountDownLatch firstWriteStarted = new CountDownLatch(1);
        CountDownLatch releaseFirstWrite = new CountDownLatch(1);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.parse("token-29")).thenReturn(new JwtClaims(29L, "u29", "USER"));
        when(session.getUri()).thenReturn(URI.create("ws://127.0.0.1:8085/ws/chat?token=token-29"));
        when(session.getId()).thenReturn("session-29");
        when(session.isOpen()).thenReturn(true);
        doAnswer(invocation -> {
            if (!writing.compareAndSet(false, true)) {
                concurrentWrites.incrementAndGet();
                throw new IllegalStateException("concurrent write");
            }
            firstWriteStarted.countDown();
            releaseFirstWrite.await(2, TimeUnit.SECONDS);
            writing.set(false);
            return null;
        }).when(session).sendMessage(any(TextMessage.class));

        ChatWebSocketHandler handler = new ChatWebSocketHandler(redisTemplate, jwtUtil);
        handler.afterConnectionEstablished(session);

        var executor = Executors.newFixedThreadPool(2);
        try {
            var first = executor.submit(() -> handler.sendToUser(29L, "first"));
            assertThat(firstWriteStarted.await(1, TimeUnit.SECONDS)).isTrue();
            var second = executor.submit(() -> handler.sendToUser(29L, "second"));

            Thread.sleep(100);
            releaseFirstWrite.countDown();
            first.get(1, TimeUnit.SECONDS);
            second.get(1, TimeUnit.SECONDS);
        } finally {
            executor.shutdownNow();
        }

        assertThat(concurrentWrites.get()).isZero();
    }

    @Test
    void afterConnectionClosed_onlyRemovesTheClosedSession() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOperations = mock(ValueOperations.class);
        JwtUtil jwtUtil = mock(JwtUtil.class);
        WebSocketSession oldSession = mock(WebSocketSession.class);
        WebSocketSession newSession = mock(WebSocketSession.class);

        when(redisTemplate.opsForValue()).thenReturn(valueOperations);
        when(jwtUtil.parse("token-29")).thenReturn(new JwtClaims(29L, "u29", "USER"));
        when(oldSession.getUri()).thenReturn(URI.create("ws://127.0.0.1:8085/ws/chat?token=token-29"));
        when(oldSession.getId()).thenReturn("old-session");
        when(newSession.getUri()).thenReturn(URI.create("ws://127.0.0.1:8085/ws/chat?token=token-29"));
        when(newSession.getId()).thenReturn("new-session");
        when(newSession.isOpen()).thenReturn(true);

        ChatWebSocketHandler handler = new ChatWebSocketHandler(redisTemplate, jwtUtil);
        handler.afterConnectionEstablished(oldSession);
        handler.afterConnectionEstablished(newSession);
        handler.afterConnectionClosed(oldSession, org.springframework.web.socket.CloseStatus.NORMAL);
        handler.sendToUser(29L, "still connected");

        org.mockito.Mockito.verify(newSession).sendMessage(any(TextMessage.class));
        org.mockito.Mockito.verify(redisTemplate, org.mockito.Mockito.never())
                .delete(RedisKey.WEBSOCKET_ONLINE + 29L);
    }
}
