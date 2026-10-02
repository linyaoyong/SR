package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class FreezeDepositFeignRequest {
    private Long renterId;
    private Long orderId;
    private BigDecimal amount;
    private Long applicationId;
    private Long proposalId;
}
