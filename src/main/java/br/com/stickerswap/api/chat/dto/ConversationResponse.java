package br.com.stickerswap.api.chat.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        UUID otherUserId,
        String otherNickname,
        UUID stickerId,
        String stickerNumber,
        String stickerName,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {}
