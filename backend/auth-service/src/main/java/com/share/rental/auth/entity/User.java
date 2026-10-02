package com.share.rental.auth.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.share.rental.common.config.BaseEntity;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.time.LocalDateTime;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("users")
public class User extends BaseEntity {
    private String username;
    private String password;
    private String avatarUrl;
    private String description;
    private Integer creditScore;
    private Integer role;
    private Integer status;
    private Integer usernameAuditStatus;
    private Integer avatarAuditStatus;
    private Integer descriptionAuditStatus;
    private Integer showRentalHistory;
    private LocalDateTime lastLoginTime;
}
