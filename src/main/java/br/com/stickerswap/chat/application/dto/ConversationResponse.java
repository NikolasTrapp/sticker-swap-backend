package br.com.stickerswap.chat.application.dto;

import java.time.Instant;
import java.util.UUID;

public record ConversationResponse(
        UUID conversationId,
        UUID otherUserId,
        String otherNickname,
        UUID stickerId,
        String stickerNumber,
        String stickerName,
        Instant createdAt,
        Instant updatedAt
) {}
