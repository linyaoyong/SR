package com.share.rental.item.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.item.dto.FavoriteResponse;
import com.share.rental.item.service.FavoriteService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/favorites")
public class FavoriteController {

    private final FavoriteService favoriteService;

    @Autowired
    public FavoriteController(FavoriteService favoriteService) {
        this.favoriteService = favoriteService;
    }

    @GetMapping
    public ApiResponse<List<FavoriteResponse>> list(@RequestHeader("X-User-Id") Long userId) {
        return ApiResponse.success(favoriteService.list(userId));
    }

    @PostMapping("/{itemId}")
    public ApiResponse<Void> favorite(@RequestHeader("X-User-Id") Long userId,
                                      @PathVariable Long itemId) {
        favoriteService.favorite(userId, itemId);
        return ApiResponse.success();
    }

    @DeleteMapping("/{itemId}")
    public ApiResponse<Void> unfavorite(@RequestHeader("X-User-Id") Long userId,
                                        @PathVariable Long itemId) {
        favoriteService.unfavorite(userId, itemId);
        return ApiResponse.success();
    }
}
