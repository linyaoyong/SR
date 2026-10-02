package com.share.rental.wallet.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@TableName("deposit_freezes")
public class DepositFreeze {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long applicationId;
    private Long proposalId;
    private Long orderId;
    private Long userId;
    private BigDecimal amount;
    private BigDecimal deductedAmount;
    private Integer status;
    private LocalDateTime freezeTime;
    private LocalDateTime releaseTime;
    private LocalDateTime cancelTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
