package com.share.rental.message.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.message.dto.CardMessageRequest;
import com.share.rental.message.dto.SystemNotificationRequest;
import com.share.rental.message.service.ChatService;
import com.share.rental.message.service.SystemNotificationService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

/**
 * 内部 Feign 接口入口，供其他服务调用 message-service。
 * - POST /internal/messages/system: 写入系统/审核通知消息记录（admin-service 审核物品后调用）
 * - POST /internal/messages/cards: 写入卡片消息（rental-service 租借申请后调用，发给物主）
 */
@RestController
public class MessageFeignController {

    private final SystemNotificationService systemNotificationService;
    private final ChatService chatService;

    @Autowired
    public MessageFeignController(SystemNotificationService systemNotificationService,
                                  ChatService chatService) {
        this.systemNotificationService = systemNotificationService;
        this.chatService = chatService;
    }

    @PostMapping("/internal/messages/system")
    public ApiResponse<Void> createSystemNotification(@RequestBody SystemNotificationRequest request) {
        systemNotificationService.createSystemNotification(request);
        return ApiResponse.success();
    }

    @PostMapping("/internal/messages/cards")
    public ApiResponse<Void> createCardMessage(@RequestBody CardMessageRequest request) {
        chatService.createCardMessage(request);
        return ApiResponse.success();
    }
}
