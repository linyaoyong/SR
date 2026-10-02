package com.share.rental.rental.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.CardMessageRequest;
import com.share.rental.rental.dto.SystemNotificationRequest;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "message-service", contextId = "messageRentalClient")
public interface MessageRentalClient {

    @PostMapping("/internal/messages/system")
    ApiResponse<Void> sendSystemNotification(@RequestBody SystemNotificationRequest request);

    @PostMapping("/internal/messages/cards")
    ApiResponse<Void> createCardMessage(@RequestBody CardMessageRequest request);
}
