package com.share.rental.rental.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReturnOrderRequest {
    @NotBlank
    @Size(max = 255)
    private String returnCompany;
    @NotBlank
    @Size(max = 255)
    private String returnTrackingNo;
}
