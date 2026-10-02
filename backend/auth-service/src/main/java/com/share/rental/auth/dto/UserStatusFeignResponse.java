package com.share.rental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserStatusFeignResponse {
    private Long id;
    private Integer status;
    private Integer creditScore;
    private Integer role;
}
