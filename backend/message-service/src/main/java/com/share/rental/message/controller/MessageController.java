package com.share.rental.message.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.upload.UploadedFile;
import com.share.rental.message.dto.OpenConversationRequest;
import com.share.rental.message.dto.SendMessageRequest;
import com.share.rental.message.service.ChatService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.Map;

@RestController
@RequestMapping("/api/messages")
public class MessageController {

    private final ChatService chatService;

    public MessageController(ChatService chatService) {
        this.chatService = chatService;
    }

    @GetMapping("/conversations")
    public ApiResponse<?> listConversations(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(chatService.listConversations(userId));
    }

    @PostMapping("/conversations/open")
    public ApiResponse<?> openConversation(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody OpenConversationRequest request) {
        return ApiResponse.success(chatService.openConversation(userId, request.getTargetUserId(), request.getItemId()));
    }

    @GetMapping("/conversations/{id}/messages")
    public ApiResponse<?> listMessages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int size) {
        return ApiResponse.success(chatService.listMessages(id, userId, page, size));
    }

    @PostMapping("/conversations/{id}/messages")
    public ApiResponse<?> sendMessage(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody SendMessageRequest request) {
        return ApiResponse.success(chatService.sendMessage(id, userId, request));
    }

    @PutMapping("/conversations/{id}/read")
    public ApiResponse<Void> markAsRead(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        chatService.markAsRead(id, userId);
        return ApiResponse.success();
    }

    @GetMapping("/unread-count")
    public ApiResponse<?> getUnreadCount(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(chatService.getUnreadCount(userId));
    }

    /**
     * 上传聊天图片。文件落地到 uploads/messages/ 目录，返回可访问的 URL。
     * 前端拿到 url 后通过 sendMessage(messageType=2, imageUrls=JSON数组字符串) 发送图片消息。
     */
    @PostMapping("/images")
    public ApiResponse<Map<String, String>> uploadImage(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam("file") MultipartFile file) {
        UploadedFile uploaded = chatService.uploadMessageImage(userId, file);
        return ApiResponse.success(Map.of("url", uploaded.url()));
    }
}
