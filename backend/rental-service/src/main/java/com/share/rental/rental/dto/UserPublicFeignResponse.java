package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * auth-service 内部接口 /internal/users/{id}/public 返回 DTO 的本地副本。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserPublicFeignResponse {
    private Long id;
    private String username;
    private String avatarUrl;
    private Integer status;
    private Integer creditScore;
}
