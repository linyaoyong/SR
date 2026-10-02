package com.share.rental.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CancelOrderPaymentRequest {
    private Long renterId;
    private Long ownerId;
    private Long orderId;
    private BigDecimal paidRentAmount;
    private BigDecimal frozenDepositAmount;
}
