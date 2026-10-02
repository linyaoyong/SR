package com.share.rental.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ItemSummary {
    private Long id;
    private Long ownerId;
    private String title;
    private String firstImageUrl;
}
