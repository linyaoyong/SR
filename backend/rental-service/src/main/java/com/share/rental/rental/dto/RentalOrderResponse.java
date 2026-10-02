package com.share.rental.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentalOrderResponse {
    private Long id;
    private String orderNo;
    private Long applicationId;
    private Long proposalId;
    private Long itemId;
    private Long itemSnapshotId;
    private String itemSnapshotTitle;
    private String itemSnapshotDescription;
    private String itemSnapshotCategoryName;
    private String itemSnapshotImageUrls;
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
    private LocalDateTime createTime;
}
