package com.share.rental.message.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.redis.RedisKey;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.upload.ImageUploadService;
import com.share.rental.common.upload.UploadedFile;
import com.share.rental.message.client.AuthMessageClient;
import com.share.rental.message.client.ItemMessageClient;
import com.share.rental.message.dto.CardMessageRequest;
import com.share.rental.message.dto.ConversationResponse;
import com.share.rental.message.dto.ItemSummary;
import com.share.rental.message.dto.MessageResponse;
import com.share.rental.message.dto.SendMessageRequest;
import com.share.rental.message.dto.UnreadCountResponse;
import com.share.rental.message.dto.UserPublicSummary;
import com.share.rental.message.entity.Conversation;
import com.share.rental.message.entity.Message;
import com.share.rental.message.mapper.ConversationMapper;
import com.share.rental.message.mapper.MessageMapper;
import com.share.rental.message.websocket.ChatWebSocketHandler;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ChatService {

    private static final Logger log = LoggerFactory.getLogger(ChatService.class);
    private final ConversationMapper conversationMapper;
    private final MessageMapper messageMapper;
    private final ChatWebSocketHandler webSocketHandler;
    private final ObjectMapper objectMapper;
    private final StringRedisTemplate redisTemplate;
    private final AuthMessageClient authMessageClient;
    private final ItemMessageClient itemMessageClient;
    private final ImageUploadService imageUploadService;

    public ChatService(ConversationMapper conversationMapper, MessageMapper messageMapper,
                       ChatWebSocketHandler webSocketHandler, ObjectMapper objectMapper,
                       StringRedisTemplate redisTemplate,
                       AuthMessageClient authMessageClient,
                       ItemMessageClient itemMessageClient,
                       ImageUploadService imageUploadService) {
        this.conversationMapper = conversationMapper;
        this.messageMapper = messageMapper;
        this.webSocketHandler = webSocketHandler;
        this.objectMapper = objectMapper;
        this.redisTemplate = redisTemplate;
        this.authMessageClient = authMessageClient;
        this.itemMessageClient = itemMessageClient;
        this.imageUploadService = imageUploadService;
    }

    public List<ConversationResponse> listConversations(Long userId) {
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserAId, userId)
                        .or()
                        .eq(Conversation::getUserBId, userId)
                        .orderByDesc(Conversation::getLastMessageTime));

        return conversations.stream().map(c -> toConversationResponse(c, userId)).collect(Collectors.toList());
    }

    public ConversationResponse openConversation(Long userId, Long targetUserId, Long itemId) {
        if (userId == null || targetUserId == null || itemId == null || userId.equals(targetUserId)) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        Long userAId = Math.min(userId, targetUserId);
        Long userBId = Math.max(userId, targetUserId);
        Conversation existing = conversationMapper.selectOne(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getItemId, itemId)
                        .eq(Conversation::getUserAId, userAId)
                        .eq(Conversation::getUserBId, userBId)
                        .last("LIMIT 1"));
        if (existing != null) {
            return toConversationResponse(existing, userId);
        }
        Conversation conversation = new Conversation();
        conversation.setItemId(itemId);
        conversation.setUserAId(userAId);
        conversation.setUserBId(userBId);
        conversation.setUserAUnreadCount(0);
        conversation.setUserBUnreadCount(0);
        conversationMapper.insert(conversation);
        return toConversationResponse(conversation, userId);
    }

    /**
     * 发送卡片消息（内部接口）。
     * 用于租借申请等业务事件触发后，给目标用户在对话列表里展示一张卡片消息。
     * 复用 openConversation 拿到（或新建）会话，再调用 sendMessage 完成消息持久化与推送。
     */
    public MessageResponse createCardMessage(CardMessageRequest request) {
        if (request == null
                || request.getSenderId() == null
                || request.getReceiverId() == null
                || request.getItemId() == null
                || request.getSenderId().equals(request.getReceiverId())) {
            throw new BusinessException(ErrorCode.BAD_REQUEST);
        }
        ConversationResponse openedConversation = openConversation(
                request.getSenderId(), request.getReceiverId(), request.getItemId());

        SendMessageRequest sendRequest = new SendMessageRequest();
        sendRequest.setMessageType(3);
        sendRequest.setCardType(request.getCardType() == null ? 1 : request.getCardType());
        sendRequest.setContent(request.getContent());
        sendRequest.setRelatedApplicationId(request.getRelatedApplicationId());
        sendRequest.setRelatedOrderId(request.getRelatedOrderId());

        return sendMessage(openedConversation.getId(), request.getSenderId(), sendRequest);
    }

    public List<MessageResponse> listMessages(Long conversationId, Long userId, int page, int size) {
        Conversation conversation = requireParticipantConversation(conversationId, userId);
        int offset = (page - 1) * size;
        List<Message> messages = messageMapper.selectList(
                new LambdaQueryWrapper<Message>()
                        .eq(Message::getConversationId, conversation.getId())
                        .orderByDesc(Message::getCreateTime)
                        .last("LIMIT " + size + " OFFSET " + offset));

        return messages.stream().map(this::toMessageResponse).collect(Collectors.toList());
    }

    public MessageResponse sendMessage(Long conversationId, Long senderId, SendMessageRequest request) {
        Conversation conversation = requireParticipantConversation(conversationId, senderId);

        Long receiverId = senderId.equals(conversation.getUserAId())
                ? conversation.getUserBId()
                : conversation.getUserAId();

        Message message = new Message();
        message.setConversationId(conversationId);
        message.setSenderId(senderId);
        message.setReceiverId(receiverId);
        message.setMessageType(request.getMessageType());
        message.setCardType(request.getCardType());
        message.setContent(request.getContent());
        message.setImageUrls(request.getImageUrls());
        message.setRelatedApplicationId(request.getRelatedApplicationId());
        message.setRelatedOrderId(request.getRelatedOrderId());
        message.setIsRead(0);
        messageMapper.insert(message);

        // Update conversation
        conversation.setLastMessageContent(request.getContent() != null ? request.getContent() : "[图片]");
        conversation.setLastMessageTime(LocalDateTime.now());

        // Increment receiver unread count
        if (receiverId.equals(conversation.getUserBId())) {
            int current = conversation.getUserBUnreadCount() == null ? 0 : conversation.getUserBUnreadCount();
            conversation.setUserBUnreadCount(current + 1);
        } else {
            int current = conversation.getUserAUnreadCount() == null ? 0 : conversation.getUserAUnreadCount();
            conversation.setUserAUnreadCount(current + 1);
        }
        conversationMapper.updateById(conversation);
        incrementUnreadCache(receiverId);

        MessageResponse response = toMessageResponse(message);

        // Push via WebSocket if receiver is online
        try {
            String json = objectMapper.writeValueAsString(response);
            webSocketHandler.sendToUser(receiverId, json);
        } catch (Exception e) {
            // WebSocket push failure should not block message persistence
            log.warn("WebSocket push failed for receiverId={}: {}", receiverId, e.getMessage());
        }

        return response;
    }

    public void markAsRead(Long conversationId, Long userId) {
        Conversation conversation = requireParticipantConversation(conversationId, userId);

        boolean isUserA = userId.equals(conversation.getUserAId());
        if (isUserA && conversation.getUserAUnreadCount() != null && conversation.getUserAUnreadCount() > 0) {
            conversation.setUserAUnreadCount(0);
            conversationMapper.updateById(conversation);
        } else if (!isUserA && conversation.getUserBUnreadCount() != null && conversation.getUserBUnreadCount() > 0) {
            conversation.setUserBUnreadCount(0);
            conversationMapper.updateById(conversation);
        }

        // Mark messages as read
        messageMapper.update(null, new UpdateWrapper<Message>()
                .eq("conversation_id", conversationId)
                .eq("receiver_id", userId)
                .eq("is_read", 0)
                .set("is_read", 1)
                .set("read_time", LocalDateTime.now()));
        syncUnreadCache(userId);
        pushReadReceipt(conversation, userId);
    }

    private void pushReadReceipt(Conversation conversation, Long readerId) {
        Long targetUserId = readerId.equals(conversation.getUserAId())
                ? conversation.getUserBId()
                : conversation.getUserAId();
        try {
            String json = objectMapper.writeValueAsString(Map.of(
                    "type", "READ_RECEIPT",
                    "conversationId", conversation.getId(),
                    "readerId", readerId));
            webSocketHandler.sendToUser(targetUserId, json);
        } catch (Exception e) {
            log.warn("WebSocket read receipt push failed for readerId={}, conversationId={}: {}",
                    readerId, conversation.getId(), e.getMessage());
        }
    }

    private Conversation requireParticipantConversation(Long conversationId, Long userId) {
        Conversation conversation = conversationMapper.selectById(conversationId);
        if (conversation == null
                || (!userId.equals(conversation.getUserAId()) && !userId.equals(conversation.getUserBId()))) {
            throw new BusinessException(ErrorCode.FORBIDDEN);
        }
        return conversation;
    }

    public UnreadCountResponse getUnreadCount(Long userId) {
        String cached = redisTemplate.opsForValue().get(unreadKey(userId));
        if (cached != null) {
            try {
                return new UnreadCountResponse(Integer.parseInt(cached));
            } catch (NumberFormatException ignored) {
                redisTemplate.delete(unreadKey(userId));
            }
        }
        int total = computeUnreadCount(userId);
        writeUnreadCache(userId, total);
        return new UnreadCountResponse(total);
    }

    /**
     * 上传聊天图片到 uploads/messages/ 目录。
     * 文件落地、压缩、命名由 common.ImageUploadService 统一处理，message-service 只负责选 bucket。
     */
    public UploadedFile uploadMessageImage(Long userId, org.springframework.web.multipart.MultipartFile file) {
        if (userId == null) {
            throw new BusinessException(ErrorCode.UNAUTHORIZED);
        }
        if (file == null || file.isEmpty()) {
            throw new BusinessException(ErrorCode.BAD_REQUEST, "图片不能为空");
        }
        return imageUploadService.storeImage(file, "messages");
    }

    private int computeUnreadCount(Long userId) {
        List<Conversation> conversations = conversationMapper.selectList(
                new LambdaQueryWrapper<Conversation>()
                        .eq(Conversation::getUserAId, userId)
                        .or()
                        .eq(Conversation::getUserBId, userId));

        int total = 0;
        for (Conversation c : conversations) {
            if (userId.equals(c.getUserAId()) && c.getUserAUnreadCount() != null) {
                total += c.getUserAUnreadCount();
            } else if (userId.equals(c.getUserBId()) && c.getUserBUnreadCount() != null) {
                total += c.getUserBUnreadCount();
                }
            }
        return total;
    }

    private void incrementUnreadCache(Long userId) {
        redisTemplate.opsForValue().increment(unreadKey(userId));
    }

    private void syncUnreadCache(Long userId) {
        writeUnreadCache(userId, computeUnreadCount(userId));
    }

    private void writeUnreadCache(Long userId, int total) {
        String key = unreadKey(userId);
        if (total > 0) {
            redisTemplate.opsForValue().set(key, String.valueOf(total));
        } else {
            redisTemplate.delete(key);
        }
    }

    private String unreadKey(Long userId) {
        return RedisKey.MESSAGE_UNREAD + userId;
    }

    private ConversationResponse toConversationResponse(Conversation c, Long userId) {
        ConversationResponse resp = new ConversationResponse();
        resp.setId(c.getId());
        resp.setItemId(c.getItemId());
        resp.setUserAId(c.getUserAId());
        resp.setUserBId(c.getUserBId());
        resp.setLastMessageContent(c.getLastMessageContent());
        resp.setLastMessageTime(c.getLastMessageTime());
        int unread = 0;
        if (userId.equals(c.getUserAId()) && c.getUserAUnreadCount() != null) {
            unread = c.getUserAUnreadCount();
        } else if (userId.equals(c.getUserBId()) && c.getUserBUnreadCount() != null) {
            unread = c.getUserBUnreadCount();
        }
        resp.setUnreadCount(unread);

        Long itemId = c.getItemId();
        Long peerUserId = userId.equals(c.getUserAId()) ? c.getUserBId() : c.getUserAId();
        resp.setPeerUserId(peerUserId);

        boolean isItemConversation = itemId != null && itemId > 0;
        boolean isPeerConversation = peerUserId != null && peerUserId > 0;

        if (isItemConversation) {
            try {
                ApiResponse<ItemSummary> itemResp = itemMessageClient.getInfo(itemId);
                if (itemResp != null && itemResp.code() == 0 && itemResp.data() != null) {
                    resp.setItemTitle(itemResp.data().getTitle());
                    resp.setItemFirstImageUrl(itemResp.data().getFirstImageUrl());
                } else {
                    resp.setItemTitle("物品 #" + itemId);
                    resp.setItemFirstImageUrl(null);
                    log.warn("Item summary fallback for itemId={}: bad response", itemId);
                }
            } catch (Exception e) {
                resp.setItemTitle("物品 #" + itemId);
                resp.setItemFirstImageUrl(null);
                log.warn("Item summary fallback for itemId={}: {}", itemId, e.getMessage());
            }
        } else {
            resp.setItemTitle(null);
            resp.setItemFirstImageUrl(null);
        }

        if (isPeerConversation) {
            try {
                ApiResponse<UserPublicSummary> userResp = authMessageClient.getPublic(peerUserId);
                if (userResp != null && userResp.code() == 0 && userResp.data() != null) {
                    resp.setPeerUsername(userResp.data().getUsername());
                    resp.setPeerAvatarUrl(userResp.data().getAvatarUrl());
                } else {
                    resp.setPeerUsername("用户 #" + peerUserId);
                    resp.setPeerAvatarUrl(null);
                    log.warn("User summary fallback for peerUserId={}: bad response", peerUserId);
                }
            } catch (Exception e) {
                resp.setPeerUsername("用户 #" + peerUserId);
                resp.setPeerAvatarUrl(null);
                log.warn("User summary fallback for peerUserId={}: {}", peerUserId, e.getMessage());
            }
        } else {
            // System notification conversation (itemId=0/null and peerUserId=0/null)
            resp.setPeerUserId(0L);
            resp.setPeerUsername("系统通知");
            resp.setPeerAvatarUrl(null);
        }
        return resp;
    }

    private MessageResponse toMessageResponse(Message m) {
        MessageResponse resp = new MessageResponse();
        resp.setId(m.getId());
        resp.setConversationId(m.getConversationId());
        resp.setSenderId(m.getSenderId());
        resp.setReceiverId(m.getReceiverId());
        resp.setMessageType(m.getMessageType());
        resp.setCardType(m.getCardType());
        resp.setContent(m.getContent());
        resp.setImageUrls(m.getImageUrls());
        resp.setRelatedApplicationId(m.getRelatedApplicationId());
        resp.setRelatedOrderId(m.getRelatedOrderId());
        resp.setIsRead(m.getIsRead());
        resp.setCreateTime(m.getCreateTime());
        return resp;
    }
}
