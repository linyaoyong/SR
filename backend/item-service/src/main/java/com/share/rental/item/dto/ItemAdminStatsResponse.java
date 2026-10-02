package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/** 管理端物品统计响应（供 admin-service 通过 Feign 调用）。 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class ItemAdminStatsResponse {
    /** 物品总数（排除逻辑删除） */
    private Long totalItems;
}
