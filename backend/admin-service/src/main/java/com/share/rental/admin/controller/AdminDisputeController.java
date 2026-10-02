package com.share.rental.admin.controller;

import com.share.rental.admin.client.RentalAdminClient;
import com.share.rental.admin.dto.ResolveDisputeRequest;
import com.share.rental.common.response.ApiResponse;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * 管理端异议管理接口。
 * admin id 由网关 GatewayAuthFilter 写入 X-User-Id 头，前端 POST/PUT 时由 service 层透传。
 * 这里直接转发给 rental-service RentalFeignController 处理。
 */
@RestController
@RequestMapping("/api/admin/disputes")
public class AdminDisputeController {

    private final RentalAdminClient rentalAdminClient;

    public AdminDisputeController(RentalAdminClient rentalAdminClient) {
        this.rentalAdminClient = rentalAdminClient;
    }

    @GetMapping
    public ApiResponse<?> listDisputes(@RequestParam(value = "status", required = false) Integer status) {
        return rentalAdminClient.listDisputes(status);
    }

    @PutMapping("/{id}/resolve")
    public ApiResponse<Void> resolveDispute(
            @RequestHeader("X-User-Id") Long adminId,
            @PathVariable Long id,
            @Valid @RequestBody ResolveDisputeRequest request) {
        request.setAdminId(adminId);
        return rentalAdminClient.resolveDispute(id, request);
    }
}
