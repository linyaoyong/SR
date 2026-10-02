package com.share.rental.wallet.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.wallet.dto.RechargeRequest;
import com.share.rental.wallet.dto.WalletMeResponse;
import com.share.rental.wallet.dto.WalletTransactionResponse;
import com.share.rental.wallet.service.WalletService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/wallet")
public class WalletController {

    private final WalletService walletService;

    @Autowired
    public WalletController(WalletService walletService) {
        this.walletService = walletService;
    }

    @GetMapping("/me")
    public ApiResponse<WalletMeResponse> me(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(walletService.getMyWallet(userId));
    }

    @PostMapping("/recharge")
    public ApiResponse<Void> recharge(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody RechargeRequest request) {
        walletService.recharge(userId, request.getAmount(), request.getRemark());
        return ApiResponse.success();
    }

    @GetMapping("/transactions")
    public ApiResponse<List<WalletTransactionResponse>> transactions(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(required = false) Integer page,
            @RequestParam(required = false) Integer size) {
        return ApiResponse.success(walletService.listTransactions(userId, page, size));
    }
}
