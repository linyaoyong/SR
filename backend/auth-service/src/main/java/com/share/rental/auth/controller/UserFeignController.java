package com.share.rental.auth.controller;

import com.share.rental.auth.dto.BanUserRequest;
import com.share.rental.auth.dto.CreditScoreAdjustRequest;
import com.share.rental.auth.dto.UserAdminStatsResponse;
import com.share.rental.auth.dto.UserAuditItemResponse;
import com.share.rental.auth.dto.UserAuditRequest;
import com.share.rental.auth.dto.UserPublicFeignResponse;
import com.share.rental.auth.dto.UserStatusFeignResponse;
import com.share.rental.auth.entity.User;
import com.share.rental.auth.service.UserService;
import com.share.rental.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/internal")
public class UserFeignController {

    private final UserService userService;

    @Autowired
    public UserFeignController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users/{id}/public")
    public ApiResponse<UserPublicFeignResponse> getPublic(@PathVariable Long id) {
        User user = userService.getUserEntity(id);
        return ApiResponse.success(new UserPublicFeignResponse(
                user.getId(),
                user.getUsername(),
                user.getAvatarUrl(),
                user.getStatus(),
                user.getCreditScore()
        ));
    }

    @GetMapping("/users/{id}/status")
    public ApiResponse<UserStatusFeignResponse> getStatus(@PathVariable Long id) {
        User user = userService.getUserEntity(id);
        return ApiResponse.success(new UserStatusFeignResponse(
                user.getId(),
                user.getStatus(),
                user.getCreditScore(),
                user.getRole()
        ));
    }

    @GetMapping("/admin/users/audits")
    public ApiResponse<List<UserAuditItemResponse>> listUserAudits(
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return ApiResponse.success(userService.listUserAudits(auditStatus));
    }

    @GetMapping("/admin/users/stats")
    public ApiResponse<UserAdminStatsResponse> userStats() {
        return ApiResponse.success(userService.getAdminStats());
    }

    @PostMapping("/admin/users/{id}/audit")
    public ApiResponse<Void> auditUser(
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody UserAuditRequest request) {
        userService.auditUser(adminId, id, request);
        return ApiResponse.success();
    }

    @PostMapping("/admin/users/{id}/ban")
    public ApiResponse<Void> banUser(
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) BanUserRequest request) {
        userService.banUser(adminId, id, request != null ? request : new BanUserRequest(null));
        return ApiResponse.success();
    }

    @PostMapping("/admin/users/{id}/unban")
    public ApiResponse<Void> unbanUser(
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id) {
        userService.unbanUser(adminId, id);
        return ApiResponse.success();
    }

    @PostMapping("/users/{id}/credit-score")
    public ApiResponse<Void> adjustCreditScore(
            @PathVariable Long id,
            @Valid @RequestBody CreditScoreAdjustRequest request) {
        userService.adjustCreditScore(id, request);
        return ApiResponse.success();
    }
}
