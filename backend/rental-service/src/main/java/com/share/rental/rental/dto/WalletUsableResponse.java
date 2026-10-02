package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * wallet-service 内部接口 /internal/wallet/users/{userId}/usable 返回 DTO 的本地副本。
 */
@Data
@AllArgsConstructor
public class WalletUsableResponse {
    private Long userId;
    private Boolean usable;
    private BigDecimal balance;
}
