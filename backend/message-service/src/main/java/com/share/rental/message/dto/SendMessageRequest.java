package com.share.rental.message.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class SendMessageRequest {
    @NotNull
    private Integer messageType; // 1=文本 2=图片 3=卡片
    private String content;
    private String imageUrls; // JSON array string
    private Integer cardType; // 1=申请 2=协商 3=订单 4=评价提醒 5=异议
    private Long relatedApplicationId;
    private Long relatedOrderId;
}
