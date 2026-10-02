package com.share.rental.rental.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.rental.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_orders")
public class RentalOrder extends BaseEntity {
    private String orderNo;
    private Long applicationId;
    private Long proposalId;
    private Long itemId;
    private Long itemSnapshotId;
    private Long ownerId;
    private Long renterId;
    private Integer quantity;
    private Integer deliveryType;
    private LocalDateTime rentStartTime;
    private LocalDateTime rentEndTime;
    private BigDecimal dailyPrice;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private BigDecimal paidRentAmount;
    private BigDecimal frozenDepositAmount;
    private Integer status;
    private String shipCompany;
    private String shipTrackingNo;
    private String returnCompany;
    private String returnTrackingNo;
    private LocalDateTime receivedTime;
    private LocalDateTime returnedTime;
    private LocalDateTime completedTime;
    private Integer overdueMinutes;
    private BigDecimal overdueFeeAmount;
    private Integer overdueSettled;
    private String cancelReason;
}
