package com.share.rental.rental.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class ShipOrderRequest {
    @NotBlank
    private String shipCompany;
    @NotBlank
    private String shipTrackingNo;
}
