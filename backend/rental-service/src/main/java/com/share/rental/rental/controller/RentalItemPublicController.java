package com.share.rental.rental.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.ReviewResponse;
import com.share.rental.rental.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rentals/items/{itemId}")
public class RentalItemPublicController {

    private final ReviewService reviewService;

    public RentalItemPublicController(ReviewService reviewService) {
        this.reviewService = reviewService;
    }

    @GetMapping("/reviews")
    public ApiResponse<List<ReviewResponse>> reviews(@PathVariable Long itemId) {
        return ApiResponse.success(reviewService.getPublicReviewsForItem(itemId));
    }
}
