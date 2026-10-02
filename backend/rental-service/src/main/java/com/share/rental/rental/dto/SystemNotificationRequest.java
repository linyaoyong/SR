package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * message-service 内部接口 /internal/messages/system 请求 DTO 的本地副本。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class SystemNotificationRequest {
    private Long receiverId;
    private String content;
    private Integer messageType;
}
