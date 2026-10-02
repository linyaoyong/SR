package com.share.rental.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("audit_records")
public class AuditRecord {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String targetType;
    private Long targetId;
    private String fieldName;
    private Integer oldStatus;
    private Integer newStatus;
    private String reason;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
