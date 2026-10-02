package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemInfoFeignResponse {
    private Long id;
    private Long ownerId;
    private String title;
    private Integer quantity;
    private Integer rentedCount;
    private BigDecimal dailyPrice;
    private BigDecimal depositAmount;
    private Integer minRentDays;
    private Integer status;
    private Integer auditStatus;
    private String firstImageUrl;
}
