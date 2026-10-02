package com.share.rental.auth.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UpdateUserProfileRequest {
    @Size(min = 3, max = 20, message = "用户名长度需为 3-20 个字符")
    private String username;
    @Size(max = 255, message = "简介长度不能超过 255")
    private String description;
    private Integer showRentalHistory;
}
