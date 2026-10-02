package com.share.rental.admin.dto;

import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * 管理端封禁用户请求体。
 */
@Data
@AllArgsConstructor
@NoArgsConstructor
public class AdminBanRequest {

    @Size(max = 255, message = "封禁原因长度不能超过 255")
    private String reason;
}
