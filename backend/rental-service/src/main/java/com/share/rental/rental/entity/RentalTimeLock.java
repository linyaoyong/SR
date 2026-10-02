package com.share.rental.rental.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("rental_time_locks")
public class RentalTimeLock {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long orderId;
    private Long itemId;
    private Integer quantity;
    private LocalDateTime rentStartTime;
    private LocalDateTime rentEndTime;
    private Integer status;
    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
    @TableField(fill = FieldFill.INSERT_UPDATE)
    private LocalDateTime updateTime;
}
