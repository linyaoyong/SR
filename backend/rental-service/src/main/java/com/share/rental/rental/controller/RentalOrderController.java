package com.share.rental.rental.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.CancelOrderRequest;
import com.share.rental.rental.dto.FreezeDepositOrderRequest;
import com.share.rental.rental.dto.PayOrderRequest;
import com.share.rental.rental.dto.RentalOrderResponse;
import com.share.rental.rental.dto.ReturnOrderRequest;
import com.share.rental.rental.dto.ShipOrderRequest;
import com.share.rental.rental.service.RentalOrderService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/orders")
public class RentalOrderController {

    private final RentalOrderService rentalOrderService;

    @Autowired
    public RentalOrderController(RentalOrderService rentalOrderService) {
        this.rentalOrderService = rentalOrderService;
    }

    @GetMapping
    public ApiResponse<List<RentalOrderResponse>> list(
            @RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(rentalOrderService.listOrders(userId));
    }

    @GetMapping("/{id}")
    public ApiResponse<RentalOrderResponse> detail(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(rentalOrderService.getOrder(id, userId));
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<RentalOrderResponse> pay(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody PayOrderRequest request) {
        return ApiResponse.success(rentalOrderService.payOrder(id, userId, request.getAmount()));
    }

    @PostMapping("/{id}/freeze-deposit")
    public ApiResponse<RentalOrderResponse> freezeDeposit(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody FreezeDepositOrderRequest request) {
        return ApiResponse.success(rentalOrderService.freezeDeposit(id, userId, request.getAmount()));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Void> cancel(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) CancelOrderRequest request) {
        rentalOrderService.cancel(id, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<Void> ship(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ShipOrderRequest request) {
        rentalOrderService.ship(id, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/receive")
    public ApiResponse<Void> receive(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        rentalOrderService.receive(id, userId);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/return")
    public ApiResponse<Void> returnOrder(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody(required = false) ReturnOrderRequest request) {
        rentalOrderService.returnOrder(id, userId, request);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/complete")
    public ApiResponse<Void> complete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        rentalOrderService.complete(id, userId);
        return ApiResponse.success();
    }
}
