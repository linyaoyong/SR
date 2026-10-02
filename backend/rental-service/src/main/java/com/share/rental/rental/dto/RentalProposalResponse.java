package com.share.rental.rental.dto;

import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentalProposalResponse {
    private Long id;
    private Long applicationId;
    private Integer versionNo;
    private Long operatorId;
    private Integer quantity;
    private Integer deliveryType;
    private LocalDateTime rentStartTime;
    private LocalDateTime rentEndTime;
    private LocalDateTime meetupTime;
    private String meetupLocation;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private String changedFields;
    private String remark;
    private LocalDateTime createTime;
}
