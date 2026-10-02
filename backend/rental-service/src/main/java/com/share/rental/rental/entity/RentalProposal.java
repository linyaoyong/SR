package com.share.rental.rental.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("rental_proposals")
public class RentalProposal {
    @TableId(type = IdType.AUTO)
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
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
