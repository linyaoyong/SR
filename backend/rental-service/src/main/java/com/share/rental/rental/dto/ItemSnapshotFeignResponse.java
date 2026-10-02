package com.share.rental.rental.dto;

import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemSnapshotFeignResponse {
    private Long id;
    private Long itemId;
    private Long ownerId;
    private String title;
    private String description;
    private String categoryName;
    private String imageUrls;
    private Integer priceType;
    private BigDecimal dailyPrice;
    private BigDecimal depositAmount;
    private Integer supportDelivery;
    private Integer supportMeetup;
}
