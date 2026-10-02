package com.share.rental.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class WalletMeResponse {
    private Long userId;
    private BigDecimal balance;
    private BigDecimal frozenAmount;
    private Integer status;
}
