package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class FavoriteResponse {
    private Long id;
    private Long itemId;
    private String itemTitle;
    private BigDecimal dailyPrice;
    private String firstImageUrl;
    private LocalDateTime createTime;
}
