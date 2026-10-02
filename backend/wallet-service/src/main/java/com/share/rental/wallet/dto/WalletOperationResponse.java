package com.share.rental.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WalletOperationResponse {
    private Long orderId;
    private BigDecimal paidRentAmount;
    private BigDecimal frozenDepositAmount;
    private Boolean orderFullyPaid;
}
