package com.share.rental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserPublicResponse {
    private Long id;
    private String username;
    private String avatarUrl;
    private String description;
    private Integer creditScore;
    private Integer status;
    private Integer showRentalHistory;
}
