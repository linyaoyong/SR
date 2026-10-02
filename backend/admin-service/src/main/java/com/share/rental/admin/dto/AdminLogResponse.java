package com.share.rental.admin.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

/**
 * 管理端操作日志查询响应体。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminLogResponse {

    private Long id;
    private Long adminId;
    private String operationType;
    private String targetType;
    private Long targetId;
    private String remark;
    private String ip;
    private LocalDateTime createTime;
}
