package com.share.rental.item.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.ItemAdminStatsResponse;
import com.share.rental.item.dto.ItemAuditActionResponse;
import com.share.rental.item.dto.ItemAuditRequest;
import com.share.rental.item.dto.ItemAuditResponse;
import com.share.rental.item.dto.ItemInfoFeignResponse;
import com.share.rental.item.dto.ItemSnapshotFeignResponse;
import com.share.rental.item.service.ItemInternalService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 内部 Feign 接口入口，供其他服务调用 item-service。
 * - /internal/items/{id}/info: 物品基础信息（rental-service 使用）
 * - /internal/items/{id}/snapshots: 创建物品快照（rental-service 下单时使用）
 * - /internal/items/snapshots/{id}: 读取物品快照（rental-service 订单详情使用）
 * - /internal/admin/items/audits: 审核列表（admin-service 使用）
 * - /internal/admin/items/{id}/audit: 审核物品（admin-service 使用）
 * - /internal/admin/items/{id}/force-off-shelf: 强制下架（admin-service 使用）
 */
@RestController
public class ItemFeignController {

    private final ItemInternalService itemInternalService;

    @Autowired
    public ItemFeignController(ItemInternalService itemInternalService) {
        this.itemInternalService = itemInternalService;
    }

    @GetMapping("/internal/items/{id}/info")
    public ApiResponse<ItemInfoFeignResponse> info(@PathVariable Long id) {
        return ApiResponse.success(itemInternalService.getItemInfo(id));
    }

    @PostMapping("/internal/items/{id}/snapshots")
    public ApiResponse<Long> snapshots(@PathVariable Long id) {
        return ApiResponse.success(itemInternalService.createSnapshot(id));
    }

    @PostMapping("/internal/items/{id}/rentals/reserve")
    public ApiResponse<Void> reserveRentedCount(
            @PathVariable Long id,
            @RequestParam("quantity") Integer quantity) {
        itemInternalService.reserveRentedCount(id, quantity);
        return ApiResponse.success();
    }

    @PostMapping("/internal/items/{id}/rentals/release")
    public ApiResponse<Void> releaseRentedCount(
            @PathVariable Long id,
            @RequestParam("quantity") Integer quantity) {
        itemInternalService.releaseRentedCount(id, quantity);
        return ApiResponse.success();
    }

    @GetMapping("/internal/items/snapshots/{id}")
    public ApiResponse<ItemSnapshotFeignResponse> snapshot(@PathVariable Long id) {
        return ApiResponse.success(itemInternalService.getSnapshot(id));
    }

    @GetMapping("/internal/admin/items/audits")
    public ApiResponse<List<ItemAuditResponse>> audits(
            @RequestParam(value = "auditStatus", required = false) Integer auditStatus) {
        return ApiResponse.success(itemInternalService.listAuditItems(auditStatus));
    }

    @GetMapping("/internal/admin/items/stats")
    public ApiResponse<ItemAdminStatsResponse> itemStats() {
        return ApiResponse.success(itemInternalService.getAdminStats());
    }

    @PostMapping("/internal/admin/items/{id}/audit")
    public ApiResponse<ItemAuditActionResponse> audit(
            @PathVariable Long id,
            @RequestHeader("X-User-Id") Long adminId,
            @Valid @RequestBody ItemAuditRequest request) {
        return ApiResponse.success(itemInternalService.auditItem(
                id, request.getAuditStatus(), request.getAuditReason(), adminId));
    }

    @PostMapping("/internal/admin/items/{id}/force-off-shelf")
    public ApiResponse<Void> forceOffShelf(@PathVariable Long id) {
        itemInternalService.forceOffShelf(id);
        return ApiResponse.success();
    }
}
