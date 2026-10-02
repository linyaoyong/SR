package com.share.rental.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class WalletTransactionResponse {
    private Long id;
    private String transactionNo;
    private Integer type;
    private Integer direction;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private String remark;
    private LocalDateTime createTime;
}
