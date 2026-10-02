package com.share.rental.message.websocket;

import com.share.rental.common.redis.RedisKey;
import com.share.rental.common.security.JwtClaims;
import com.share.rental.common.security.JwtUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.web.socket.CloseStatus;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;
import org.springframework.web.socket.handler.ConcurrentWebSocketSessionDecorator;
import org.springframework.web.socket.handler.TextWebSocketHandler;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;

@Component
public class ChatWebSocketHandler extends TextWebSocketHandler {

    private static final Logger log = LoggerFactory.getLogger(ChatWebSocketHandler.class);
    private static final String TOKEN_PARAM = "token";
    private static final int SEND_TIME_LIMIT_MILLIS = 10_000;
    private static final int SEND_BUFFER_SIZE_LIMIT_BYTES = 512 * 1024;

    private final StringRedisTemplate redisTemplate;
    private final JwtUtil jwtUtil;

    // userId -> session (local instance)
    private final Map<Long, WebSocketSession> sessionMap = new ConcurrentHashMap<>();

    public ChatWebSocketHandler(StringRedisTemplate redisTemplate, JwtUtil jwtUtil) {
        this.redisTemplate = redisTemplate;
        this.jwtUtil = jwtUtil;
    }

    @Override
    public void afterConnectionEstablished(WebSocketSession session) throws Exception {
        Long userId = extractUserId(session);
        if (userId == null) {
            session.close(CloseStatus.POLICY_VIOLATION);
            return;
        }
        WebSocketSession sendSafeSession = new ConcurrentWebSocketSessionDecorator(
                session,
                SEND_TIME_LIMIT_MILLIS,
                SEND_BUFFER_SIZE_LIMIT_BYTES);
        sessionMap.put(userId, sendSafeSession);
        redisTemplate.opsForValue().set(
                RedisKey.WEBSOCKET_ONLINE + userId,
                session.getId(),
                300, TimeUnit.SECONDS);
        log.info("WebSocket connected: userId={}, sessionId={}", userId, session.getId());
    }

    @Override
    protected void handleTextMessage(WebSocketSession session, TextMessage message) throws Exception {
        String payload = message.getPayload();
        // Heartbeat: client sends "ping", server responds "pong" and refreshes online ttl
        if ("ping".equalsIgnoreCase(payload.trim())) {
            Long userId = extractUserId(session);
            if (userId != null) {
                redisTemplate.opsForValue().set(
                        RedisKey.WEBSOCKET_ONLINE + userId,
                        session.getId(),
                        300, TimeUnit.SECONDS);
            }
            WebSocketSession sendSession = currentSessionFor(session, userId);
            if (sendSession != null && sendSession.isOpen()) {
                sendSession.sendMessage(new TextMessage("pong"));
            }
            return;
        }
        // Other messages are ignored (clients use REST API to send messages)
    }

    @Override
    public void afterConnectionClosed(WebSocketSession session, CloseStatus status) throws Exception {
        Long userId = extractUserId(session);
        if (userId != null) {
            // 同一用户可能因前端 StrictMode double-mount 或重连而建立多个 session。
            // 只在当前 session 仍是注册的那个时才移除，避免误删新 session 导致推送丢失。
            WebSocketSession currentSession = sessionMap.get(userId);
            if (currentSession != null && session.getId().equals(currentSession.getId())) {
                sessionMap.remove(userId);
                redisTemplate.delete(RedisKey.WEBSOCKET_ONLINE + userId);
            }
            log.info("WebSocket disconnected: userId={}, status={}", userId, status);
        }
    }

    public void sendToUser(Long userId, String message) {
        WebSocketSession session = sessionMap.get(userId);
        if (session != null && session.isOpen()) {
            try {
                session.sendMessage(new TextMessage(message));
            } catch (Exception e) {
                log.error("Failed to send WebSocket message to userId={}", userId, e);
            }
        }
    }

    public boolean isUserOnline(Long userId) {
        return sessionMap.containsKey(userId) || Boolean.TRUE.equals(
                redisTemplate.hasKey(RedisKey.WEBSOCKET_ONLINE + userId));
    }

    private WebSocketSession currentSessionFor(WebSocketSession session, Long userId) {
        WebSocketSession currentSession = sessionMap.get(userId);
        if (currentSession != null && session.getId().equals(currentSession.getId())) {
            return currentSession;
        }
        return session;
    }

    private Long extractUserId(WebSocketSession session) {
        URI uri = session.getUri();
        if (uri == null) return null;
        String query = uri.getQuery();
        if (query == null) return null;
        for (String param : query.split("&")) {
            String[] pair = param.split("=", 2);
            if (TOKEN_PARAM.equals(pair[0]) && pair.length == 2) {
                return parseUserIdFromToken(pair[1]);
            }
        }
        return null;
    }

    private Long parseUserIdFromToken(String token) {
        // Gateway already validated the JWT for /ws/** route. For direct connection
        // without gateway, parse JWT here using common JwtUtil.
        try {
            JwtClaims claims = jwtUtil.parse(token);
            return claims.userId();
        } catch (Exception e) {
            log.warn("Failed to parse userId from token: {}", e.getMessage());
            return null;
        }
    }
}
