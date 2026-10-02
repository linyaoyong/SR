package com.share.rental.rental.controller;

import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.dto.RentalOrderResponse;
import com.share.rental.rental.dto.ReviewResponse;
import com.share.rental.rental.service.RentalOrderService;
import com.share.rental.rental.service.ReviewService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/rentals/users/{userId}")
public class RentalUserPublicController {

    private final RentalOrderService rentalOrderService;
    private final ReviewService reviewService;

    public RentalUserPublicController(RentalOrderService rentalOrderService, ReviewService reviewService) {
        this.rentalOrderService = rentalOrderService;
        this.reviewService = reviewService;
    }

    @GetMapping("/history")
    public ApiResponse<List<RentalOrderResponse>> history(@PathVariable Long userId) {
        return ApiResponse.success(rentalOrderService.listPublicCompletedOrders(userId));
    }

    @GetMapping("/reviews")
    public ApiResponse<List<ReviewResponse>> reviews(@PathVariable Long userId) {
        return ApiResponse.success(reviewService.getPublicReviewsForUser(userId));
    }
}
