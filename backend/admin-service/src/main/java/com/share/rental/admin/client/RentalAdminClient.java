package com.share.rental.admin.client;

import com.share.rental.admin.dto.DisputeResponse;
import com.share.rental.admin.dto.ResolveDisputeRequest;
import com.share.rental.common.response.ApiResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用 rental-service 的异议管理内部接口（管理员查看异议列表、处理异议）。
 * 对应 rental-service RentalFeignController 的 /internal/rental/disputes/** 路径。
 */
@FeignClient(name = "rental-service", path = "/internal/rental/disputes", contextId = "rentalAdminClient")
public interface RentalAdminClient {

    @GetMapping
    ApiResponse<List<DisputeResponse>> listDisputes(@RequestParam(value = "status", required = false) Integer status);

    @PutMapping("/{id}/resolve")
    ApiResponse<Void> resolveDispute(@PathVariable("id") Long id, @RequestBody ResolveDisputeRequest request);
}
