package com.share.rental.rental.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.upload.ImageUploadService;
import com.share.rental.common.upload.UploadedFile;
import com.share.rental.rental.dto.ReviewCreateRequest;
import com.share.rental.rental.service.ReviewService;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/orders/{orderId}/reviews")
public class ReviewController {

    private final ReviewService reviewService;
    private final ImageUploadService imageUploadService;

    public ReviewController(ReviewService reviewService, ImageUploadService imageUploadService) {
        this.reviewService = reviewService;
        this.imageUploadService = imageUploadService;
    }

    @PostMapping
    public ApiResponse<?> createReview(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId,
            @Valid @RequestBody ReviewCreateRequest request) {
        return ApiResponse.success(reviewService.createReview(orderId, userId, request));
    }

    @GetMapping
    public ApiResponse<?> getReviews(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId) {
        return ApiResponse.success(reviewService.getReviews(orderId, userId));
    }

    @PostMapping("/images")
    public ApiResponse<UploadedFile> uploadReviewImage(
            @RequestHeader("X-User-Id") Long userId,
            @PathVariable Long orderId,
            @RequestParam("file") MultipartFile file) {
        reviewService.getReviews(orderId, userId);
        return ApiResponse.success(imageUploadService.storeImage(file, "reviews"));
    }
}
