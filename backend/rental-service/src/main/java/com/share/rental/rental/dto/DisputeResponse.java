package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

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
