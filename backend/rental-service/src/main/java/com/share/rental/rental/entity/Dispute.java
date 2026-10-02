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
@TableName("disputes")
public class Dispute {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long applicantId;
    private String reason;
    private String description;
    private BigDecimal expectedDepositDeduction;
    private String imageUrls;
    private Integer status;
    private Long adminId;
    private String adminRemark;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
