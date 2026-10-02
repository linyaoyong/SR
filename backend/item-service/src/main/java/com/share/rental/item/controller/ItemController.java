package com.share.rental.item.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.response.PageResult;
import com.share.rental.item.dto.ItemCreateRequest;
import com.share.rental.item.dto.ItemDetailResponse;
import com.share.rental.item.dto.ItemImageResponse;
import com.share.rental.item.dto.ItemListResponse;
import com.share.rental.item.dto.ItemUpdateRequest;
import com.share.rental.item.service.ItemService;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/api/items")
public class ItemController {

    private final ItemService itemService;

    @Autowired
    public ItemController(ItemService itemService) {
        this.itemService = itemService;
    }

    @GetMapping
    public ApiResponse<PageResult<ItemListResponse>> list(
            @RequestParam(value = "categoryId", required = false) Long categoryId,
            @RequestParam(value = "keyword", required = false) String keyword,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(itemService.listPublic(categoryId, keyword, page, size));
    }

    @GetMapping("/mine")
    public ApiResponse<PageResult<ItemListResponse>> mine(
            @RequestHeader("X-User-Id") Long userId,
            @RequestParam(value = "page", defaultValue = "1") int page,
            @RequestParam(value = "size", defaultValue = "20") int size) {
        return ApiResponse.success(itemService.listMine(userId, page, size));
    }

    @GetMapping("/{id}")
    public ApiResponse<ItemDetailResponse> detail(
            @RequestHeader(value = "X-User-Id", required = false) Long userId,
            @PathVariable Long id) {
        return ApiResponse.success(itemService.detail(userId, id));
    }

    @PostMapping
    public ApiResponse<ItemDetailResponse> create(
            @RequestHeader("X-User-Id") Long userId,
            @Valid @RequestBody ItemCreateRequest request) {
        return ApiResponse.success(itemService.create(userId, request));
    }

    @PutMapping("/{id}")
    public ApiResponse<Void> update(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @Valid @RequestBody ItemUpdateRequest request) {
        itemService.update(userId, id, request);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/off-shelf")
    public ApiResponse<Void> offShelf(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        itemService.offShelf(userId, id);
        return ApiResponse.success();
    }

    @PutMapping("/{id}/re-list")
    public ApiResponse<Void> reList(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        itemService.reList(userId, id);
        return ApiResponse.success();
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id) {
        itemService.delete(userId, id);
        return ApiResponse.success();
    }

    @PostMapping("/{id}/images")
    public ApiResponse<List<ItemImageResponse>> addImages(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long id,
            @RequestParam("file") List<MultipartFile> files) {
        return ApiResponse.success(itemService.addImages(userId, id, files));
    }
}
