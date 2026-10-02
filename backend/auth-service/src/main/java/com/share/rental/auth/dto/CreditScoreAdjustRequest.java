package com.share.rental.auth.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreditScoreAdjustRequest {
    @NotNull
    private Long orderId;
    @NotNull
    private Integer changeValue;
    @NotBlank
    private String reasonType;
    private String reason;
}
