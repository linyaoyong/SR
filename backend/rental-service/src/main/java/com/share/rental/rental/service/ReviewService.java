package com.share.rental.rental.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.rental.client.AuthRentalClient;
import com.share.rental.rental.dto.CreditScoreAdjustFeignRequest;
import com.share.rental.rental.dto.ReviewCreateRequest;
import com.share.rental.rental.dto.ReviewResponse;
import com.share.rental.rental.dto.UserPublicFeignResponse;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.entity.Review;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.enums.ReviewStatusEnum;
import com.share.rental.rental.mapper.RentalOrderMapper;
import com.share.rental.rental.mapper.ReviewMapper;
import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class ReviewService {

    private final RentalOrderMapper orderMapper;
    private final ReviewMapper reviewMapper;
    private final AuthRentalClient authClient;

    public ReviewService(RentalOrderMapper orderMapper, ReviewMapper reviewMapper, AuthRentalClient authClient) {
        this.orderMapper = orderMapper;
        this.reviewMapper = reviewMapper;
        this.authClient = authClient;
    }

    public ReviewResponse createReview(Long orderId, Long reviewerId, ReviewCreateRequest request) {
        RentalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        if (order.getStatus() == null || order.getStatus() != OrderStatusEnum.COMPLETED.code()) {
            throw new BusinessException(ErrorCode.REVIEW_ORDER_NOT_COMPLETED);
        }
        if (!reviewerId.equals(order.getRenterId()) && !reviewerId.equals(order.getOwnerId())) {
            throw new BusinessException(ErrorCode.REVIEW_PERMISSION_DENIED);
        }

        Review existing = reviewMapper.selectOne(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getOrderId, orderId)
                        .eq(Review::getReviewerId, reviewerId));

        Long revieweeId = reviewerId.equals(order.getRenterId())
                ? order.getOwnerId()
                : order.getRenterId();

        if (existing != null) {
            Integer oldRating = existing.getRating();
            existing.setRating(request.getRating());
            existing.setContent(request.getContent());
            existing.setImageUrls(request.getImageUrls());
            existing.setStatus(ReviewStatusEnum.NORMAL.code());
            reviewMapper.updateById(existing);
            adjustLowRatingCredit(existing.getRevieweeId(), orderId, oldRating, request.getRating());
            return toResponse(existing);
        }

        Review review = new Review();
        review.setOrderId(orderId);
        review.setItemId(order.getItemId());
        review.setReviewerId(reviewerId);
        review.setRevieweeId(revieweeId);
        review.setRating(request.getRating());
        review.setContent(request.getContent());
        review.setImageUrls(request.getImageUrls());
        review.setStatus(ReviewStatusEnum.NORMAL.code());
        reviewMapper.insert(review);
        adjustLowRatingCredit(revieweeId, orderId, null, request.getRating());

        return toResponse(review);
    }

    private void adjustLowRatingCredit(Long revieweeId, Long orderId, Integer oldRating, Integer newRating) {
        boolean wasLow = oldRating != null && oldRating <= 2;
        boolean isLow = newRating != null && newRating <= 2;
        if (!wasLow && isLow) {
            adjustCreditSafely(revieweeId, orderId, -2, "LOW_RATING_REVIEW", "收到 1-2 星差评");
        } else if (wasLow && !isLow) {
            adjustCreditSafely(revieweeId, orderId, 2, "LOW_RATING_REVIEW_UPDATED", "差评已更新");
        }
    }

    private void adjustCreditSafely(Long userId, Long orderId, int changeValue, String reasonType, String reason) {
        try {
            authClient.adjustCreditScore(userId,
                    new CreditScoreAdjustFeignRequest(orderId, changeValue, reasonType, reason));
        } catch (Exception ignored) {
            // 信用分调整失败不影响评价提交，后续可通过信用分记录补偿。
        }
    }

    public List<ReviewResponse> getReviews(Long orderId, Long userId) {
        RentalOrder order = orderMapper.selectById(orderId);
        if (order == null) {
            throw new BusinessException(ErrorCode.RENTAL_ORDER_NOT_FOUND);
        }
        if (!userId.equals(order.getRenterId()) && !userId.equals(order.getOwnerId())) {
            throw new BusinessException(ErrorCode.REVIEW_PERMISSION_DENIED);
        }

        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>().eq(Review::getOrderId, orderId));
        return reviews.stream().map(this::toResponse).collect(Collectors.toList());
    }

    public List<ReviewResponse> getPublicReviewsForUser(Long userId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getRevieweeId, userId)
                        .eq(Review::getStatus, ReviewStatusEnum.NORMAL.code())
                        .orderByDesc(Review::getCreateTime)
                        .last("LIMIT 20"));
        Map<Long, String> reviewerNameCache = new HashMap<>();
        return reviews.stream()
                .map(r -> toResponseWithName(r, reviewerNameCache))
                .collect(Collectors.toList());
    }

    public List<ReviewResponse> getPublicReviewsForItem(Long itemId) {
        List<Review> reviews = reviewMapper.selectList(
                new LambdaQueryWrapper<Review>()
                        .eq(Review::getItemId, itemId)
                        .eq(Review::getStatus, ReviewStatusEnum.NORMAL.code())
                        .orderByDesc(Review::getCreateTime)
                        .last("LIMIT 50"));
        Map<Long, String> reviewerNameCache = new HashMap<>();
        return reviews.stream()
                .map(r -> toResponseWithName(r, reviewerNameCache))
                .collect(Collectors.toList());
    }

    private ReviewResponse toResponseWithName(Review r, Map<Long, String> reviewerNameCache) {
        ReviewResponse resp = toResponse(r);
        resp.setReviewerName(reviewerNameCache.computeIfAbsent(r.getReviewerId(), this::fetchReviewerName));
        return resp;
    }

    private String fetchReviewerName(Long reviewerId) {
        try {
            ApiResponse<UserPublicFeignResponse> resp = authClient.getPublic(reviewerId);
            if (resp != null && resp.data() != null) {
                return resp.data().getUsername();
            }
        } catch (Exception ignored) {
            // 评价者用户名查询失败不阻断评价列表展示，前端可降级显示用户 ID。
        }
        return null;
    }

    private ReviewResponse toResponse(Review r) {
        ReviewResponse resp = new ReviewResponse();
        resp.setId(r.getId());
        resp.setOrderId(r.getOrderId());
        resp.setItemId(r.getItemId());
        resp.setReviewerId(r.getReviewerId());
        resp.setRevieweeId(r.getRevieweeId());
        resp.setRating(r.getRating());
        resp.setContent(r.getContent());
        resp.setImageUrls(r.getImageUrls());
        resp.setStatus(r.getStatus());
        resp.setCreateTime(r.getCreateTime());
        return resp;
    }
}
