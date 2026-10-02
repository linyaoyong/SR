package com.share.rental.auth.controller;

import com.share.rental.auth.dto.BlacklistRequest;
import com.share.rental.auth.dto.BlacklistResponse;
import com.share.rental.auth.dto.UpdatePasswordRequest;
import com.share.rental.auth.dto.UpdateUserProfileRequest;
import com.share.rental.auth.dto.UserMeResponse;
import com.share.rental.auth.dto.UserPublicResponse;
import com.share.rental.auth.service.BlacklistService;
import com.share.rental.auth.service.UserService;
import com.share.rental.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;
    private final BlacklistService blacklistService;

    @Autowired
    public UserController(UserService userService, BlacklistService blacklistService) {
        this.userService = userService;
        this.blacklistService = blacklistService;
    }

    @GetMapping("/me")
    public ApiResponse<UserMeResponse> me(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(userService.getMe(userId));
    }

    @PutMapping("/me")
    public ApiResponse<UserMeResponse> updateProfile(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdateUserProfileRequest request) {
        return ApiResponse.success(userService.updateProfile(userId, request));
    }

    @PutMapping("/me/password")
    public ApiResponse<Void> updatePassword(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody UpdatePasswordRequest request) {
        userService.updatePassword(userId, request);
        return ApiResponse.success();
    }

    @GetMapping("/{id}")
    public ApiResponse<UserPublicResponse> publicProfile(@PathVariable Long id) {
        return ApiResponse.success(userService.getPublic(id));
    }

    @PostMapping("/blacklist/{targetUserId}")
    public ApiResponse<Void> blacklist(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId,
            @Valid @RequestBody(required = false) BlacklistRequest request) {
        String reason = request != null ? request.getReason() : null;
        blacklistService.blacklist(userId, targetUserId, reason);
        return ApiResponse.success();
    }

    @DeleteMapping("/blacklist/{targetUserId}")
    public ApiResponse<Void> unblacklist(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long targetUserId) {
        blacklistService.unblacklist(userId, targetUserId);
        return ApiResponse.success();
    }

    @GetMapping("/blacklist")
    public ApiResponse<List<BlacklistResponse>> listBlacklist(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(blacklistService.listBlacklist(userId));
    }
}
