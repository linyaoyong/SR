package com.share.rental.message.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.message.dto.UserPublicSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", contextId = "authMessageClient")
public interface AuthMessageClient {
    @GetMapping("/internal/users/{id}/public")
    ApiResponse<UserPublicSummary> getPublic(@PathVariable("id") Long id);
}
