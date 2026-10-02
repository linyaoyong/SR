package com.share.rental.rental.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class ReviewCreateRequest {
    @NotNull
    @Min(1) @Max(5)
    private Integer rating;
    @Size(max = 500)
    private String content;
    private String imageUrls;
}
