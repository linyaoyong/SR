package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

@Data
@AllArgsConstructor
public class WalletUsableResponse {
    private Long userId;
    private Boolean usable;
    private BigDecimal balance;
}
