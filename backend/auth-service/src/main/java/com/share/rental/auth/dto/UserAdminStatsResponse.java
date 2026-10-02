package com.share.rental.auth.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 管理端用户统计响应（供 admin-service 通过 Feign 调用）。 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class UserAdminStatsResponse {
    /** 用户总数（含普通用户和管理员，排除逻辑删除） */
    private Long totalUsers;
    /** 已封禁用户数（status=1） */
    private Long bannedUsers;
}
