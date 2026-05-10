package br.com.stickerswap.api.chat.dto;

import br.com.stickerswap.domain.chat.model.MessageType;

import java.time.LocalDateTime;
import java.util.UUID;

public record MessageResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderUserId,
        MessageType type,
        String body,
        LocalDateTime sentAt
) {}
