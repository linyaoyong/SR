package com.share.rental.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class MessageResponse {
    private Long id;
    private Long conversationId;
    private Long senderId;
    private Long receiverId;
    private Integer messageType;
    private Integer cardType;
    private String content;
    private String imageUrls;
    private Long relatedApplicationId;
    private Long relatedOrderId;
    private Integer isRead;
    private LocalDateTime createTime;
}
