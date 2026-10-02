package com.share.rental.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class BlacklistRequest {
    @Size(max = 255, message = "拉黑理由长度不能超过 255")
    private String reason;
}
