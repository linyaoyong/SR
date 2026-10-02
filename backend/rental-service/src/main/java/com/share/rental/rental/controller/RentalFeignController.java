package com.share.rental.rental.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.DisputeResponse;
import com.share.rental.rental.dto.ResolveDisputeFeignRequest;
import com.share.rental.rental.service.DisputeService;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
public class RentalFeignController {

    private final DisputeService disputeService;

    public RentalFeignController(DisputeService disputeService) {
        this.disputeService = disputeService;
    }

    @GetMapping("/internal/rental/disputes")
    public ApiResponse<List<DisputeResponse>> listAllDisputes(
            @RequestParam(required = false) Integer status) {
        return ApiResponse.success(disputeService.listAllDisputes(status));
    }

    @PutMapping("/internal/rental/disputes/{id}/resolve")
    public ApiResponse<Void> resolveDispute(
            @PathVariable Long id,
            @RequestBody ResolveDisputeFeignRequest request) {
        disputeService.resolveDispute(id, request);
        return ApiResponse.success();
    }
}
