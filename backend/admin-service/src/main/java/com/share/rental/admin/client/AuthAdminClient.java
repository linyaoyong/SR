package com.share.rental.admin.client;

import com.share.rental.auth.dto.BanUserRequest;
import com.share.rental.auth.dto.UserAdminStatsResponse;
import com.share.rental.auth.dto.UserAuditItemResponse;
import com.share.rental.auth.dto.UserAuditRequest;
import com.share.rental.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用 auth-service 的内部管理接口（用户资料审核、封禁、解禁）。
 * 对应 auth-service UserFeignController 的 /internal/admin/users/** 路径。
 */
@FeignClient(name = "auth-service", path = "/internal/admin/users", contextId = "authAdminClient")
public interface AuthAdminClient {

    @GetMapping("/audits")
    ApiResponse<List<UserAuditItemResponse>> listAudits(
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus);

    @GetMapping("/stats")
    ApiResponse<UserAdminStatsResponse> getStats();

    @PostMapping("/{id}/audit")
    ApiResponse<Void> auditUser(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody UserAuditRequest request);

    @PostMapping("/{id}/ban")
    ApiResponse<Void> banUser(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody BanUserRequest request);

    @PostMapping("/{id}/unban")
    ApiResponse<Void> unbanUser(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long adminId);
}
