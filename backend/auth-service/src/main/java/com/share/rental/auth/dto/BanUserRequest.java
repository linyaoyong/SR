package com.share.rental.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class BanUserRequest {
    @Size(max = 255, message = "封禁原因长度不能超过 255")
    private String reason;
}
