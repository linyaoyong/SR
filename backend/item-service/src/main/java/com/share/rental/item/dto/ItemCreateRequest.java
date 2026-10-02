package com.share.rental.item.dto;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class ItemCreateRequest {
    @NotBlank
    @Size(min = 2, max = 60)
    private String title;
    @NotBlank
    @Size(min = 1, max = 2000)
    private String description;
    @NotNull
    private Long categoryId;
    private String tags;
    @NotNull
    @Min(1)
    private Integer quantity;
    private Integer supportDelivery;
    private String deliveryCity;
    private Integer supportMeetup;
    private String meetupLocation;
    private Integer priceType;
    @NotNull
    @DecimalMin("0")
    private BigDecimal dailyPrice;
    @Min(1)
    private Integer minRentDays;
    private Integer freeRent;
    private Integer depositEnabled;
    @DecimalMin("0")
    private BigDecimal depositAmount;
    private Integer creditDepositEnabled;
    private Integer minCreditScore;
    private Integer freeDepositScore;
    private Integer reducedDepositScore;
    @DecimalMin("0")
    private BigDecimal reducedDepositAmount;
}
