package com.share.rental.item.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class ItemImageResponse {
    private Long id;
    private String url;
    private Integer sortOrder;
}
