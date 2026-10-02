package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@AllArgsConstructor
public class ItemDetailResponse {
    private Long id;
    private Long ownerId;
    private String title;
    private String description;
    private Long categoryId;
    private String tags;
    private Integer quantity;
    private Integer rentedCount;
    private Integer supportDelivery;
    private String deliveryCity;
    private Integer supportMeetup;
    private String meetupLocation;
    private Integer priceType;
    private BigDecimal dailyPrice;
    private Integer minRentDays;
    private Integer freeRent;
    private Integer depositEnabled;
    private BigDecimal depositAmount;
    private Integer creditDepositEnabled;
    private Integer minCreditScore;
    private Integer freeDepositScore;
    private Integer reducedDepositScore;
    private BigDecimal reducedDepositAmount;
    private Integer status;
    private Integer auditStatus;
    private String auditReason;
    private List<ItemImageResponse> images;
    private LocalDateTime createTime;
}
