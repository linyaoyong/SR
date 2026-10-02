package com.share.rental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAuditRequest {
    @NotBlank(message = "审核字段名不能为空")
    private String fieldName;

    @NotNull(message = "审核状态不能为空")
    private Integer auditStatus;

    @Size(max = 255, message = "整改原因长度不能超过 255")
    private String reason;
}
