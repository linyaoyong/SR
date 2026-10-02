package com.share.rental.rental.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.RentalApplicationCreateRequest;
import com.share.rental.rental.dto.RentalApplicationDetailResponse;
import com.share.rental.rental.dto.RentalApplicationResponse;
import com.share.rental.rental.dto.RentalProposalUpdateRequest;
import com.share.rental.rental.service.RentalApplicationService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rentals/applications")
public class RentalApplicationController {

    private final RentalApplicationService rentalApplicationService;

    @Autowired
    public RentalApplicationController(RentalApplicationService rentalApplicationService) {
        this.rentalApplicationService = rentalApplicationService;
    }

    @PostMapping
    public ApiResponse<RentalApplicationDetailResponse> create(
            @RequestHeader("X-User-Id") Long renterId,
            @Valid @RequestBody RentalApplicationCreateRequest request) {
        return ApiResponse.success(rentalApplicationService.createApplication(renterId, request));
    }

    @GetMapping
    public ApiResponse<List<RentalApplicationResponse>> list(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(rentalApplicationService.listApplications(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<RentalApplicationDetailResponse> detail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(rentalApplicationService.getApplication(id, userId));
    }

    @PutMapping("/{id}/proposal")
    public ApiResponse<RentalApplicationDetailResponse> updateProposal(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody RentalProposalUpdateRequest request) {
        return ApiResponse.success(rentalApplicationService.updateProposal(id, userId, request));
    }

    @PutMapping("/{id}/confirm")
    public ApiResponse<RentalApplicationDetailResponse> confirm(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(rentalApplicationService.confirm(id, userId));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        rentalApplicationService.cancel(id, userId);
        return ApiResponse.success();
    }
}
