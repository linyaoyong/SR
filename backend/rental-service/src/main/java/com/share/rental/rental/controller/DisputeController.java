package com.share.rental.rental.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.DisputeCreateRequest;
import com.share.rental.rental.service.DisputeService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/orders/{orderId}/disputes")
public class DisputeController {

    private final DisputeService disputeService;

    public DisputeController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @PostMapping
    public ApiResponse<?> createDispute(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody DisputeCreateRequest request) {
        return ApiResponse.success(disputeService.createDispute(orderId, userId, request));
    }

    @GetMapping
    public ApiResponse<?> listDisputes(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(disputeService.listDisputes(orderId, userId));
    }
}
