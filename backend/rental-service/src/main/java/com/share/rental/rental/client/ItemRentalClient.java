package com.share.rental.rental.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.ItemInfoFeignResponse;
import com.share.rental.rental.dto.ItemSnapshotFeignResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(name = "item-service", contextId = "itemRentalClient")
public interface ItemRentalClient {

    @GetMapping("/internal/items/{id}/info")
    ApiResponse<ItemInfoFeignResponse> getItemInfo(@PathVariable Long id);

    @PostMapping("/internal/items/{id}/snapshots")
    ApiResponse<Long> createSnapshot(@PathVariable Long id);

    @PostMapping("/internal/items/{id}/rentals/reserve")
    ApiResponse<Void> reserveRentedCount(@PathVariable Long id, @RequestParam("quantity") Integer quantity);

    @PostMapping("/internal/items/{id}/rentals/release")
    ApiResponse<Void> releaseRentedCount(@PathVariable Long id, @RequestParam("quantity") Integer quantity);

    @GetMapping("/internal/items/snapshots/{id}")
    ApiResponse<ItemSnapshotFeignResponse> getSnapshot(@PathVariable Long id);
}
