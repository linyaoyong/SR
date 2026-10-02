package com.share.rental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@AllArgsConstructor
public class BlacklistResponse {
    private Long id;
    private Long targetUserId;
    private String targetUsername;
    private String reason;
    private LocalDateTime createTime;
}
