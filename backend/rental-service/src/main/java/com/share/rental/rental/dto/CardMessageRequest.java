package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * message-service 内部接口 /internal/messages/cards 请求 DTO 的本地副本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class CardMessageRequest {
    private Long senderId;
    private Long receiverId;
    private Long itemId;
    private String content;
    private Integer cardType;
    private Long relatedApplicationId;
    private Long relatedOrderId;
}
