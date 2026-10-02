package com.share.rental.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * rental-service DisputeResponse 的本地副本，admin-service 用于反序列化 Feign 响应。
 * 字段需与 com.share.rental.rental.dto.DisputeResponse 保持一致。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class DisputeResponse {
    private Long id;
    private Long orderId;
    private Long applicantId;
    private String reason;
    private String description;
    private BigDecimal expectedDepositDeduction;
    private String imageUrls;
    private Integer status;
    private Long adminId;
    private String adminRemark;
    private LocalDateTime createTime;
    private LocalDateTime updateTime;
}
