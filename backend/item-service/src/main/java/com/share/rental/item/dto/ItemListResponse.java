package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class ItemListResponse {
    private Long id;
    private String title;
    private Long categoryId;
    private BigDecimal dailyPrice;
    private Integer minRentDays;
    private BigDecimal depositAmount;
    private Integer status;
    private Integer auditStatus;
    private String firstImageUrl;
    private LocalDateTime createTime;
}
