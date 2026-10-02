package com.share.rental.admin.dto;

import lombok.Data;

/**
 * 管理员处理异议请求体。
 * adminId 由网关注入 X-User-Id 头转发；adminRemark 为可选处理意见。
 */
@Data
public class ResolveDisputeRequest {

    private Long adminId;

    private String adminRemark;
}
