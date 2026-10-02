package com.share.rental.message.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ConversationResponse {
    private Long id;
    private Long itemId;
    private Long userAId;
    private Long userBId;
    private String lastMessageContent;
    private LocalDateTime lastMessageTime;
    private Integer unreadCount;
    private String itemTitle;
    private String itemFirstImageUrl;
    private Long peerUserId;
    private String peerUsername;
    private String peerAvatarUrl;
}
