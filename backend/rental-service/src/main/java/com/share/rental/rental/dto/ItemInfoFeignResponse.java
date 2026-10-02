package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

import java.math.BigDecimal;

/**
 * item-service 内部接口 /internal/items/{id}/info 返回 DTO 的本地副本。
 * rental-service 不依赖 item-service 模块，故在此重新定义。
 */
@Data
@AllArgsConstructor
public class ItemInfoFeignResponse {
    private Long id;
    private Long ownerId;
    private String title;
    private Integer quantity;
    private Integer rentedCount;
    private BigDecimal dailyPrice;
    private BigDecimal depositAmount;
    private Integer minRentDays;
    private Integer status;
    private Integer auditStatus;
}
