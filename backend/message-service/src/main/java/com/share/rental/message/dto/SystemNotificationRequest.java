package com.share.rental.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemNotificationRequest {

    private Long receiverId;

    private String content;

    private Integer messageType;
}
