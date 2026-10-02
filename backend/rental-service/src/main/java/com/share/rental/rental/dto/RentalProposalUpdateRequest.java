package com.share.rental.rental.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentalProposalUpdateRequest {
    @Min(1)
    private Integer quantity;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rentStartTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rentEndTime;
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime meetupTime;
    private String meetupLocation;
    private String receiverName;
    private String receiverPhone;
    private String receiverAddress;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private String remark;
}
