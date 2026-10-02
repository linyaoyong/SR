package com.share.rental.rental.dto;

import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class RentalApplicationDetailResponse extends RentalApplicationResponse {
    private String itemTitle;
}
