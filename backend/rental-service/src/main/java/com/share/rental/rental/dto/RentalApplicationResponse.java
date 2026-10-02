package com.share.rental.rental.dto;

import lombok.Data;

import java.time.LocalDateTime;

@Data
public class RentalApplicationResponse {
    private Long id;
    private Long itemId;
    private Long renterId;
    private Long ownerId;
    private Integer status;
    private Long currentProposalId;
    private Integer ownerConfirmed;
    private Integer renterConfirmed;
    private LocalDateTime createTime;
    private RentalProposalResponse currentProposal;
}
