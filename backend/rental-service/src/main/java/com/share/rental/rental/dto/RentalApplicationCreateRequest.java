package com.share.rental.rental.dto;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class RentalApplicationCreateRequest {
    @NotNull
    private Long itemId;
    @NotNull
    @Min(1)
    private Integer quantity;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rentStartTime;
    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime rentEndTime;
    @NotNull
    private Integer deliveryType;
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
