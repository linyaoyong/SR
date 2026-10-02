package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

/**
 * auth-service 内部接口 /internal/users/{id}/status 返回 DTO 的本地副本。
 */
@Data
@AllArgsConstructor
public class UserStatusFeignResponse {
    private Long id;
    private Integer status;
    private Integer creditScore;
    private Integer role;
}
