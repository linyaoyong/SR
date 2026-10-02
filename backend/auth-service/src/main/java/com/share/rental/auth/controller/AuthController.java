package com.share.rental.auth.controller;

import com.share.rental.auth.dto.LoginRequest;
import com.share.rental.auth.dto.LoginResponse;
import com.share.rental.auth.dto.RefreshResponse;
import com.share.rental.auth.dto.RegisterRequest;
import com.share.rental.auth.service.UserService;
import com.share.rental.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    @Autowired
    public AuthController(UserService userService) {
        this.userService = userService;
    }

    @PostMapping("/register")
    public ApiResponse<Void> register(@Valid @RequestBody RegisterRequest request) {
        userService.register(request);
        return ApiResponse.success();
    }

    @PostMapping("/login")
    public ApiResponse<LoginResponse> login(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.login(request));
    }

    @PostMapping("/admin/login")
    public ApiResponse<LoginResponse> adminLogin(@Valid @RequestBody LoginRequest request) {
        return ApiResponse.success(userService.adminLogin(request));
    }

    @PostMapping("/refresh")
    public ApiResponse<RefreshResponse> refresh(
            @RequestHeader(value = "Authorization", required = false) String authorization) {
        return ApiResponse.success(userService.refresh(authorization));
    }
}
