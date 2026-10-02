package com.share.rental.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicSummary {
    private Long id;
    private String username;
    private String avatarUrl;
    private Integer status;
    private Integer creditScore;
}
