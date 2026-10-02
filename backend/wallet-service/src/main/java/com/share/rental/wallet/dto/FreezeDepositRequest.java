package com.share.rental.wallet.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreezeDepositRequest {
    private Long renterId;
    private Long orderId;
    private BigDecimal amount;
    private Long applicationId;
    private Long proposalId;
}
