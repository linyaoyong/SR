package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CompleteSettlementFeignRequest {
    private Long renterId;
    private Long ownerId;
    private Long orderId;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private BigDecimal frozenDepositAmount;
    private BigDecimal paidRentAmount;
    private LocalDateTime rentEndTime;
    private BigDecimal dailyPrice;
}
