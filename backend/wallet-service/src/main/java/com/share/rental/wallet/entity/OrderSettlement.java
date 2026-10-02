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
@TableName("order_settlements")
public class OrderSettlement {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long renterId;
    private Long ownerId;
    private BigDecimal rentAmount;
    private BigDecimal depositAmount;
    private BigDecimal overdueFeeAmount;
    private BigDecimal depositDeductedAmount;
    private Integer rentSettled;
    private Integer depositReleased;
    private Integer overdueFeeSettled;
    private Integer status;
    private LocalDateTime settleTime;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;

    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
