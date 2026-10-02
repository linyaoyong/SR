package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {
    private Long id;
    private Long orderId;
    private Long itemId;
    private Long reviewerId;
    private String reviewerName;
    private Long revieweeId;
    private Integer rating;
    private String content;
    private String imageUrls;
    private Integer status;
    private LocalDateTime createTime;
}
