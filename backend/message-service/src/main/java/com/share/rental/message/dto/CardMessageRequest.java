package com.share.rental.message.dto;

import lombok.Data;

@Data
public class CardMessageRequest {
    private Long senderId;
    private Long receiverId;
    private Long itemId;
    private String content;
    /** 卡片类型：1=租借申请 */
    private Integer cardType;
    private Long relatedApplicationId;
    private Long relatedOrderId;
}
