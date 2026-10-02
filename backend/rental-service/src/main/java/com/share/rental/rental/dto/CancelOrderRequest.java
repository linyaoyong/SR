package com.share.rental.rental.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class CancelOrderRequest {
    @Size(max = 255)
    private String cancelReason;
}
