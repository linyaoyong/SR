package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemAuditActionResponse {
    private Long itemId;
    private Long ownerId;
    private Integer auditStatus;
    private String auditReason;
    private LocalDateTime auditTime;
    private Long auditAdminId;
}
