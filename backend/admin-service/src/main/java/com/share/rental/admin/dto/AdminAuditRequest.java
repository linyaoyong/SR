package com.share.rental.admin.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端审核请求体。
 * - auditStatus：审核结果（1=通过, 2=整改；P0 阶段仅支持这两种状态）
 * - auditReason：整改原因（物品审核用）
 * - fieldName：审核的用户资料字段名（仅用户资料审核使用，物品审核忽略）
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminAuditRequest {

    @NotNull(message = "审核状态不能为空")
    @Min(value = 1, message = "审核状态最小为 1")
    @Max(value = 2, message = "审核状态最大为 2")
    private Integer auditStatus;

    @Size(max = 255, message = "审核原因长度不能超过 255")
    private String auditReason;

    private String fieldName;
}
