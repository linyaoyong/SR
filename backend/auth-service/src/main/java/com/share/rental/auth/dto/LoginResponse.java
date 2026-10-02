package com.share.rental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {
    private Long userId;
    private String username;
    private String role;
    private String accessToken;
    private String refreshToken;
    private Long expiresIn;
}
