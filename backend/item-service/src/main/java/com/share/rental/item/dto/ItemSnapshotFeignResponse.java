package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
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
