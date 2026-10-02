package com.share.rental.item.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.UserStatusFeignResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "auth-service", path = "/internal/users")
public interface AuthFeignClient {

    @GetMapping("/{id}/status")
    ApiResponse<UserStatusFeignResponse> getUserStatus(@PathVariable Long id);
}
