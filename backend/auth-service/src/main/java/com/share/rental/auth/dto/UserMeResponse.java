package com.share.rental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class UserMeResponse {
    private Long id;
    private String username;
    private String avatarUrl;
    private String description;
    private Integer creditScore;
    private Integer role;
    private Integer status;
    private Integer usernameAuditStatus;
    private Integer avatarAuditStatus;
    private Integer descriptionAuditStatus;
    private Integer showRentalHistory;
    private LocalDateTime lastLoginTime;
}
