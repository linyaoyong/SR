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
@TableName("wallet_transactions")
public class WalletTransaction {
    @TableId(type = IdType.AUTO)
    private Long id;
    private String transactionNo;
    private Long userId;
    private Long counterpartyUserId;
    private Long applicationId;
    private Long proposalId;
    private Long orderId;
    private Integer type;
    private Integer direction;
    private BigDecimal amount;
    private BigDecimal balanceBefore;
    private BigDecimal balanceAfter;
    private BigDecimal frozenBefore;
    private BigDecimal frozenAfter;
    private String remark;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
