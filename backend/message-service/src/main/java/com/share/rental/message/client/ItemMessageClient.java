package com.share.rental.message.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.message.dto.ItemSummary;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(name = "item-service", contextId = "itemMessageClient")
public interface ItemMessageClient {
    @GetMapping("/internal/items/{id}/info")
    ApiResponse<ItemSummary> getInfo(@PathVariable("id") Long id);
}
