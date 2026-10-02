package com.share.rental.item.entity;

import com.baomidou.mybatisplus.annotation.FieldStrategy;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableName;
import com.share.rental.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("items")
public class Item extends BaseEntity {
    private Long ownerId;
    private String title;
    private String description;
    private Long categoryId;
    private String tags;
    private Integer quantity;
    private Integer rentedCount;
    private Integer supportDelivery;
    private String deliveryCity;
    private Integer supportMeetup;
    private String meetupLocation;
    private Integer priceType;
    private BigDecimal dailyPrice;
    private Integer minRentDays;
    private Integer freeRent;
    private Integer depositEnabled;
    private BigDecimal depositAmount;
    private Integer creditDepositEnabled;
    private Integer minCreditScore;
    private Integer freeDepositScore;
    private Integer reducedDepositScore;
    private BigDecimal reducedDepositAmount;
    private Integer status;
    private Integer auditStatus;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private String auditReason;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private LocalDateTime auditTime;
    @TableField(updateStrategy = FieldStrategy.ALWAYS)
    private Long auditAdminId;
}
