package com.share.rental.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

import java.math.BigDecimal;

@Data
public class DisputeCreateRequest {
    @NotBlank
    @Size(max = 128)
    private String reason;
    @Size(max = 500)
    private String description;
    private BigDecimal expectedDepositDeduction;
    private String imageUrls;
}
