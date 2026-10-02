package com.share.rental.auth.dto;

import java.util.List;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理员审核用户列表项响应。
 *
 * <p>管理员在审核用户资料时，需要看到用户会公开展示的全部关键字段，
 * 因此本 DTO 在原有审核状态字段基础上，补充头像、简介、信用分、状态、
 * 公开设置以及各字段审核状态，避免管理端只能看到 ID 与用户名。
 *
 * <p>fieldNames 包含该用户所有匹配当前筛选条件（待审/整改/全部）的字段，
 * 便于前端一行展示全部待审字段；fieldName/auditStatus 保留为首选操作字段，
 * 供管理端"通过"/"要求整改"按钮作为默认目标，保持向后兼容。
 */
@Data
@NoArgsConstructor
@AllArgsConstructor
public class UserAuditItemResponse {
    private Long userId;
    private String username;
    /** 审核字段名：username / avatar / description（首选操作字段，对应 fieldNames 首项） */
    private String fieldName;
    private Integer auditStatus;
    /** 列表查询时恒为 null，审核原因由 admin-service audit_records 持久化 */
    private String reason;

    // ===== 扩展字段：用户公开展示与审核上下文 =====
    private String avatarUrl;
    private String description;
    private Integer creditScore;
    /** 用户状态：0=正常 1=已封禁 */
    private Integer status;
    /** 是否公开租赁历史：0=不公开 1=公开 */
    private Integer showRentalHistory;
    /** 用户名审核状态，便于详情页同时展示三字段当前状态 */
    private Integer usernameAuditStatus;
    /** 头像审核状态 */
    private Integer avatarAuditStatus;
    /** 简介审核状态 */
    private Integer descriptionAuditStatus;

    // ===== 一行展示全部待审字段 =====
    /** 该用户所有待审核（或匹配当前筛选）的字段名列表，便于一行展示全部待审字段 */
    private List<String> fieldNames;
    /** 与 fieldNames 一一对应的审核状态列表 */
    private List<Integer> auditStatuses;
}
