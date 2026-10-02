package com.share.rental.admin.controller;

import com.share.rental.admin.dto.AdminAuditRequest;
import com.share.rental.admin.dto.AdminBanRequest;
import com.share.rental.admin.dto.AdminDashboardResponse;
import com.share.rental.admin.dto.AdminLogResponse;
import com.share.rental.admin.service.AdminAuditService;
import com.share.rental.auth.dto.UserAuditItemResponse;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditResponse;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 管理端对外接口。
 * admin id 由网关 GatewayAuthFilter 写入 X-User-Id 头；
 * ADMIN 角色校验已在网关完成，admin-service 只假设 admin id 存在。
 */
@RestController
@RequestMapping("/api/admin")
public class AdminController {

    private final AdminAuditService adminAuditService;

    @Autowired
    public AdminController(AdminAuditService adminAuditService) {
        this.adminAuditService = adminAuditService;
    }

    @GetMapping("/dashboard")
    public ApiResponse<AdminDashboardResponse> dashboard() {
        return ApiResponse.success(adminAuditService.getDashboard());
    }

    @GetMapping("/users/audits")
    public ApiResponse<List<UserAuditItemResponse>> listUserAudits(
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return ApiResponse.success(adminAuditService.listUserAudits(auditStatus));
    }

    @PostMapping("/users/{id}/audit")
    public ApiResponse<Void> auditUser(
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody AdminAuditRequest request,
            HttpServletRequest httpRequest) {
        adminAuditService.auditUser(adminId, id, request, extractIp(httpRequest));
        return ApiResponse.success();
    }

    @PostMapping("/users/{id}/ban")
    public ApiResponse<Void> banUser(
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody AdminBanRequest request,
            HttpServletRequest httpRequest) {
        adminAuditService.banUser(adminId, id, request, extractIp(httpRequest));
        return ApiResponse.success();
    }

    @PostMapping("/users/{id}/unban")
    public ApiResponse<Void> unbanUser(
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminAuditService.unbanUser(adminId, id, extractIp(httpRequest));
        return ApiResponse.success();
    }

    @GetMapping("/items/audits")
    public ApiResponse<List<ItemAuditResponse>> listItemAudits(
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return ApiResponse.success(adminAuditService.listItemAudits(auditStatus));
    }

    @PostMapping("/items/{id}/audit")
    public ApiResponse<ItemAuditActionResponse> auditItem(
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody AdminAuditRequest request,
            HttpServletRequest httpRequest) {
        return ApiResponse.success(adminAuditService.auditItem(adminId, id, request, extractIp(httpRequest)));
    }

    @PostMapping("/items/{id}/force-off-shelf")
    public ApiResponse<Void> forceOffShelf(
            @RequestHeader(value = "X-User-Id", required = true) Long adminId,
            @PathVariable Long id,
            HttpServletRequest httpRequest) {
        adminAuditService.forceOffShelf(adminId, id, extractIp(httpRequest));
        return ApiResponse.success();
    }

    @GetMapping("/logs")
    public ApiResponse<List<AdminLogResponse>> listLogs(
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        // 分页边界保护：page 至少 1，size 限定 1..100，防止恶意大 size 触发 OOM
        int safePage = Math.max(1, page);
        int safeSize = Math.min(Math.max(1, size), 100);
        return ApiResponse.success(adminAuditService.listLogs(safePage, safeSize));
    }

    private String extractIp(HttpServletRequest request) {
        String forwarded = request.getHeader("X-Forwarded-For");
        if (forwarded != null && !forwarded.isBlank()) {
            int comma = forwarded.indexOf(',');
            return (comma > 0 ? forwarded.substring(0, comma) : forwarded).trim();
        }
        return request.getRemoteAddr();
    }

    /**
     * 缺少 admin id 头时返回 400，避免绕过网关直连 admin-service 时 adminId=null 写库。
     * Controller 内 @ExceptionHandler 优先于 GlobalExceptionHandler 的兜底处理。
     */
    @ExceptionHandler(MissingRequestHeaderException.class)
    public ResponseEntity<ApiResponse<Void>> handleMissingRequestHeader(MissingRequestHeaderException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(ApiResponse.error(ErrorCode.VALIDATION_ERROR, ex.getMessage()));
    }
}
