package com.share.rental.admin.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.message.dto.SystemNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

/**
 * 调用 message-service 的内部接口（写入系统/审核通知记录）。
 * 对应 message-service MessageFeignController 的 POST /internal/messages/system。
 */
@FeignClient(name = "message-service", contextId = "messageAdminClient")
public interface MessageAdminClient {

    @PostMapping("/internal/messages/system")
    ApiResponse<Void> createSystemNotification(@RequestBody SystemNotificationRequest request);
}
