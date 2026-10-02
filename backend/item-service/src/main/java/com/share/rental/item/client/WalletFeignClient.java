package com.share.rental.item.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.WalletUsableResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "wallet-service", path = "/internal/wallet")
public interface WalletFeignClient {

    @GetMapping("/users/{userId}/usable")
    ApiResponse<WalletUsableResponse> isUsable(@PathVariable Long userId);
}
