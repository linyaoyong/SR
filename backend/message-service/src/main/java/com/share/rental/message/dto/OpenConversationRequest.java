package com.share.rental.message.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class OpenConversationRequest {
    @NotNull
    private Long targetUserId;
    @NotNull
    private Long itemId;
}
