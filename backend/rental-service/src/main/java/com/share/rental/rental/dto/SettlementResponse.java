package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponse {
    private Long orderId;
    private Boolean rentSettled;
    private Boolean depositReleased;
    private BigDecimal overdueFeeAmount;
    private BigDecimal depositDeductedAmount;
}
