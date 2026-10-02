package com.share.rental.item.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ItemAuditRequest {
    @NotNull
    private Integer auditStatus;
    @Size(max = 255)
    private String auditReason;
}
