package com.share.rental.rental.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.rental.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.math.BigDecimal;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("rental_applications")
public class RentalApplication extends BaseEntity {
    private Long itemId;
    private Long ownerId;
    private Long renterId;
    private Long conversationId;
    private Integer status;
    private Long currentProposalId;
    private Integer ownerConfirmed;
    private Integer renterConfirmed;
    private BigDecimal prepaidRentAmount;
    private BigDecimal preFrozenDepositAmount;
}
