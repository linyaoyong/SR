package com.share.rental.rental.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.GlobalExceptionHandler;
import com.share.rental.common.upload.UploadProperties;
import com.share.rental.rental.dto.ReviewCreateRequest;
import com.share.rental.rental.dto.ReviewResponse;
import com.share.rental.rental.service.ReviewService;
import com.share.rental.common.upload.ImageUploadService;
import com.share.rental.common.upload.UploadedFile;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(ReviewController.class)
@Import(GlobalExceptionHandler.class)
@EnableConfigurationProperties(UploadProperties.class)
class ReviewControllerTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.web.context.WebApplicationContext gatewayFixtureContext;

    @org.junit.jupiter.api.BeforeEach
    void useExplicitGatewayCredentialFixture() {
        mvc = org.springframework.test.web.servlet.setup.MockMvcBuilders
                .webAppContextSetup(gatewayFixtureContext)
                .defaultRequest(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get("/")
                        .header("X-Internal-Token", "test-only-backend-ingress-token"))
                .build();
    }

    @Autowired
    private MockMvc mvc;
    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ReviewService reviewService;
    @MockBean
    private ImageUploadService imageUploadService;

    @Test
    void createReview_returns200() throws Exception {
        when(reviewService.createReview(eq(1L), eq(100L), any(ReviewCreateRequest.class)))
                .thenReturn(reviewResponse(10L));

        ReviewCreateRequest req = new ReviewCreateRequest();
        req.setRating(5);
        req.setContent("great item");

        mvc.perform(post("/api/orders/1/reviews")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data.id").value(10));
    }

    @Test
    void createReview_missingRating_returns400() throws Exception {
        ReviewCreateRequest req = new ReviewCreateRequest();

        mvc.perform(post("/api/orders/1/reviews")
                        .header("X-User-Id", "100")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(40004));
    }

    @Test
    void getReviews_returns200() throws Exception {
        when(reviewService.getReviews(eq(1L), eq(100L)))
                .thenReturn(List.of(reviewResponse(10L)));

        mvc.perform(get("/api/orders/1/reviews")
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(0))
                .andExpect(jsonPath("$.data[0].id").value(10));
    }

    @Test
    void uploadReviewImage_returnsStoredReviewFileUrl() throws Exception {
        when(imageUploadService.storeImage(any(), eq("reviews")))
                .thenReturn(new UploadedFile("/files/reviews/a.jpg", "a.jpg", "image/jpeg", 1234L));

        mvc.perform(org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart("/api/orders/1/reviews/images")
                        .file("file", "image".getBytes())
                        .header("X-User-Id", "100"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.url").value("/files/reviews/a.jpg"));
    }

    private ReviewResponse reviewResponse(Long id) {
        ReviewResponse resp = new ReviewResponse();
        resp.setId(id);
        resp.setOrderId(1L);
        resp.setItemId(50L);
        resp.setReviewerId(100L);
        resp.setRevieweeId(200L);
        resp.setRating(5);
        resp.setContent("great item");
        resp.setStatus(0);
        return resp;
    }
}
