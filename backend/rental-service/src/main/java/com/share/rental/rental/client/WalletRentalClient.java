package com.share.rental.rental.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.CancelPaymentFeignRequest;
import com.share.rental.rental.dto.CompleteSettlementFeignRequest;
import com.share.rental.rental.dto.FreezeDepositFeignRequest;
import com.share.rental.rental.dto.PrepayRentFeignRequest;
import com.share.rental.rental.dto.SettlementResponse;
import com.share.rental.rental.dto.WalletOperationResponse;
import com.share.rental.rental.dto.WalletUsableResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "wallet-service", contextId = "walletRentalClient")
public interface WalletRentalClient {

    @GetMapping("/internal/wallet/users/{userId}/usable")
    ApiResponse<WalletUsableResponse> isUsable(@PathVariable Long userId);

    @PostMapping("/internal/wallet/orders/{orderId}/prepay-rent")
    ApiResponse<WalletOperationResponse> prepayRent(@PathVariable Long orderId,
                                                     @RequestBody PrepayRentFeignRequest request);

    @PostMapping("/internal/wallet/orders/{orderId}/freeze-deposit")
    ApiResponse<WalletOperationResponse> freezeDeposit(@PathVariable Long orderId,
                                                       @RequestBody FreezeDepositFeignRequest request);

    @PostMapping("/internal/wallet/orders/{orderId}/cancel")
    ApiResponse<Void> cancelOrderPayment(@PathVariable Long orderId,
                                         @RequestBody CancelPaymentFeignRequest request);

    @PostMapping("/internal/wallet/orders/{orderId}/settle")
    ApiResponse<SettlementResponse> completeSettlement(@PathVariable Long orderId,
                                                       @RequestBody CompleteSettlementFeignRequest request);

    @GetMapping("/internal/wallet/orders/{orderId}/settlement")
    ApiResponse<SettlementResponse> getSettlement(@PathVariable Long orderId);
}
