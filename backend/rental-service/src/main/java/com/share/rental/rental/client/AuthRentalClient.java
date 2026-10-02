package com.share.rental.rental.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.CreditScoreAdjustFeignRequest;
import com.share.rental.rental.dto.UserPublicFeignResponse;
import com.share.rental.rental.dto.UserStatusFeignResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;

@FeignClient(name = "auth-service", contextId = "authRentalClient")
public interface AuthRentalClient {

    @GetMapping("/internal/users/{id}/status")
    ApiResponse<UserStatusFeignResponse> getUserStatus(@PathVariable Long id);

    @GetMapping("/internal/users/{id}/public")
    ApiResponse<UserPublicFeignResponse> getPublic(@PathVariable Long id);

    @PostMapping("/internal/users/{id}/credit-score")
    ApiResponse<Void> adjustCreditScore(@PathVariable Long id, @RequestBody CreditScoreAdjustFeignRequest request);
}
