package com.share.rental.admin.client;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.ItemAdminStatsResponse;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditRequest;
import com.share.rental.item.dto.ItemAuditResponse;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;

import java.util.List;

/**
 * 调用 item-service 的内部管理接口（物品审核列表、审核、强制下架）。
 * 对应 item-service ItemFeignController 的 /internal/admin/items/** 路径。
 */
@FeignClient(name = "item-service", path = "/internal/admin/items", contextId = "itemAdminClient")
public interface ItemAdminClient {

    @GetMapping("/audits")
    ApiResponse<List<ItemAuditResponse>> listAudits(
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus);

    @GetMapping("/stats")
    ApiResponse<ItemAdminStatsResponse> getStats();

    @PostMapping("/{id}/audit")
    ApiResponse<ItemAuditActionResponse> auditItem(
            @PathVariable("id") Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @RequestBody ItemAuditRequest request);

    @PostMapping("/{id}/force-off-shelf")
    ApiResponse<Void> forceOffShelf(@PathVariable("id") Long id);
}
