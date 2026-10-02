package com.share.rental.admin.entity;

import com.baomidou.mybatisplus.annotation.FieldFill;
import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("admin_operation_logs")
public class AdminOperationLog {
    @TableId(type = IdType.AUTO)
    private Long id;
    private Long adminId;
    private String operationType;
    private String targetType;
    private Long targetId;
    private String beforeJson;
    private String afterJson;
    private String remark;
    private String ip;

    @TableField(fill = FieldFill.INSERT)
    private LocalDateTime createTime;
}
