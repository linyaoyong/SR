package com.share.rental.rental.service;

import com.share.rental.common.exception.BusinessException;
import com.share.rental.rental.dto.ReviewCreateRequest;
import com.share.rental.rental.client.AuthRentalClient;
import com.share.rental.rental.dto.CreditScoreAdjustFeignRequest;
import com.share.rental.rental.entity.RentalOrder;
import com.share.rental.rental.entity.Review;
import com.share.rental.rental.enums.OrderStatusEnum;
import com.share.rental.rental.enums.ReviewStatusEnum;
import com.share.rental.rental.mapper.RentalOrderMapper;
import com.share.rental.rental.mapper.ReviewMapper;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReviewServiceTest {

    @Mock RentalOrderMapper orderMapper;
    @Mock ReviewMapper reviewMapper;
    @Mock AuthRentalClient authClient;
    @InjectMocks ReviewService reviewService;

    @Test
    void createReview_validOrder_insertsReview() {
        RentalOrder order = new RentalOrder();
        order.setId(1L);
        order.setItemId(10L);
        order.setRenterId(100L);
        order.setOwnerId(200L);
        order.setStatus(OrderStatusEnum.COMPLETED.code());
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(reviewMapper.selectOne(any())).thenReturn(null);

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setContent("great item");

        reviewService.createReview(1L, 100L, request);

        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).insert(captor.capture());
        Review review = captor.getValue();
        assertThat(review.getOrderId()).isEqualTo(1L);
        assertThat(review.getReviewerId()).isEqualTo(100L);
        assertThat(review.getRevieweeId()).isEqualTo(200L);
        assertThat(review.getRating()).isEqualTo(5);
        assertThat(review.getStatus()).isEqualTo(ReviewStatusEnum.NORMAL.code());
    }

    @Test
    void createReview_orderNotCompleted_throws() {
        RentalOrder order = new RentalOrder();
        order.setStatus(OrderStatusEnum.RENTING.code());
        when(orderMapper.selectById(1L)).thenReturn(order);

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        assertThatThrownBy(() -> reviewService.createReview(1L, 100L, request))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void createReview_alreadyReviewed_updatesOwnReview() {
        RentalOrder order = new RentalOrder();
        order.setId(1L);
        order.setItemId(10L);
        order.setStatus(OrderStatusEnum.COMPLETED.code());
        order.setRenterId(100L);
        order.setOwnerId(200L);
        when(orderMapper.selectById(1L)).thenReturn(order);
        Review existing = new Review();
        existing.setId(99L);
        existing.setOrderId(1L);
        existing.setItemId(10L);
        existing.setReviewerId(100L);
        existing.setRevieweeId(200L);
        existing.setRating(3);
        existing.setContent("旧评价");
        existing.setStatus(ReviewStatusEnum.NORMAL.code());
        when(reviewMapper.selectOne(any())).thenReturn(existing);

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(5);
        request.setContent("更新后的评价");
        request.setImageUrls("[\"/files/reviews/new.jpg\"]");

        var response = reviewService.createReview(1L, 100L, request);

        assertThat(response.getId()).isEqualTo(99L);
        assertThat(response.getRating()).isEqualTo(5);
        assertThat(response.getContent()).isEqualTo("更新后的评价");
        ArgumentCaptor<Review> captor = ArgumentCaptor.forClass(Review.class);
        verify(reviewMapper).updateById(captor.capture());
        assertThat(captor.getValue().getImageUrls()).isEqualTo("[\"/files/reviews/new.jpg\"]");
    }

    @Test
    void getReviews_returnsBothReviews() {
        RentalOrder order = new RentalOrder();
        order.setId(1L);
        order.setRenterId(100L);
        order.setOwnerId(200L);
        when(orderMapper.selectById(1L)).thenReturn(order);

        Review r1 = new Review();
        r1.setReviewerId(100L);
        r1.setRating(5);
        Review r2 = new Review();
        r2.setReviewerId(200L);
        r2.setRating(4);
        when(reviewMapper.selectList(any())).thenReturn(java.util.Arrays.asList(r1, r2));

        var result = reviewService.getReviews(1L, 100L);

        assertThat(result).hasSize(2);
    }

    @Test
    void createReview_lowRatingPenalizesRevieweeCredit() {
        RentalOrder order = new RentalOrder();
        order.setId(1L);
        order.setItemId(10L);
        order.setRenterId(100L);
        order.setOwnerId(200L);
        order.setStatus(OrderStatusEnum.COMPLETED.code());
        when(orderMapper.selectById(1L)).thenReturn(order);
        when(reviewMapper.selectOne(any())).thenReturn(null);

        ReviewCreateRequest request = new ReviewCreateRequest();
        request.setRating(2);
        request.setContent("体验较差");

        reviewService.createReview(1L, 100L, request);

        verify(authClient).adjustCreditScore(org.mockito.ArgumentMatchers.eq(200L),
                org.mockito.ArgumentMatchers.argThat((CreditScoreAdjustFeignRequest creditRequest) ->
                        Integer.valueOf(-2).equals(creditRequest.getChangeValue())
                                && "LOW_RATING_REVIEW".equals(creditRequest.getReasonType())));
    }

    @Test
    void getPublicReviewsForItem_returnsLatestNormalReviews() {
        Review newer = new Review();
        newer.setId(2L);
        newer.setItemId(10L);
        newer.setStatus(ReviewStatusEnum.NORMAL.code());
        newer.setRating(5);
        Review older = new Review();
        older.setId(1L);
        older.setItemId(10L);
        older.setStatus(ReviewStatusEnum.NORMAL.code());
        older.setRating(4);
        when(reviewMapper.selectList(any())).thenReturn(java.util.Arrays.asList(newer, older));

        var result = reviewService.getPublicReviewsForItem(10L);

        assertThat(result).extracting("id").containsExactly(2L, 1L);
    }
}
