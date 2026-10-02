package com.share.rental.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端首页概览响应体。
 * userAuditCount / itemAuditCount 反映所有审核记录的计数（P0 近似值）。
 * totalUsers / totalItems / bannedUsers 在 P0 阶段允许返回近似值（0）。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminDashboardResponse {

    private Long itemAuditCount;
    private Long userAuditCount;
    private Long totalUsers;
    private Long totalItems;
    private Long bannedUsers;
}
