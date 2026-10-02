package com.share.rental.rental.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResolveDisputeFeignRequest {
    private Long adminId;
    private String adminRemark;
}
