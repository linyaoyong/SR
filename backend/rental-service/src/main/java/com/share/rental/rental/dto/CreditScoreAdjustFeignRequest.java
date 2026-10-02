package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreditScoreAdjustFeignRequest {
    private Long orderId;
    private Integer changeValue;
    private String reasonType;
    private String reason;
}
