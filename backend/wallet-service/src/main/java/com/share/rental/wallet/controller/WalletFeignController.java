package com.share.rental.wallet.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.wallet.dto.CancelOrderPaymentRequest;
import com.share.rental.wallet.dto.CompleteSettlementRequest;
import com.share.rental.wallet.dto.FreezeDepositRequest;
import com.share.rental.wallet.dto.PrepayRequest;
import com.share.rental.wallet.dto.SettlementResponse;
import com.share.rental.wallet.dto.WalletOperationResponse;
import com.share.rental.wallet.dto.WalletUsableResponse;
import com.share.rental.wallet.service.WalletService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class WalletFeignController {

    private final WalletService walletService;

    @Autowired
    public WalletFeignController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/internal/wallet/users/{userId}/usable")
    public ApiResponse<WalletUsableResponse> usable(@PathVariable Long userId) {
        return ApiResponse.success(walletService.getUsable(userId));
    }

    @PostMapping("/internal/wallet/orders/{orderId}/prepay-rent")
    public ApiResponse<WalletOperationResponse> prepayRent(@PathVariable Long orderId,
                                                            @RequestBody PrepayRequest request) {
        request.setOrderId(orderId);
        return ApiResponse.success(walletService.prepayRent(request));
    }

    @PostMapping("/internal/wallet/orders/{orderId}/freeze-deposit")
    public ApiResponse<WalletOperationResponse> freezeDeposit(@PathVariable Long orderId,
                                                              @RequestBody FreezeDepositRequest request) {
        request.setOrderId(orderId);
        return ApiResponse.success(walletService.freezeDeposit(request));
    }

    @PostMapping("/internal/wallet/orders/{orderId}/cancel")
    public ApiResponse<WalletOperationResponse> cancelOrderPayment(@PathVariable Long orderId,
                                                                   @RequestBody CancelOrderPaymentRequest request) {
        request.setOrderId(orderId);
        return ApiResponse.success(walletService.cancelOrderPayment(request));
    }

    @PostMapping("/internal/wallet/orders/{orderId}/settle")
    public ApiResponse<SettlementResponse> completeSettlement(@PathVariable Long orderId,
                                                              @RequestBody CompleteSettlementRequest request) {
        request.setOrderId(orderId);
        return ApiResponse.success(walletService.completeSettlement(request));
    }

    @GetMapping("/internal/wallet/orders/{orderId}/settlement")
    public ApiResponse<SettlementResponse> getSettlement(@PathVariable Long orderId) {
        return ApiResponse.success(walletService.getSettlement(orderId));
    }
}
