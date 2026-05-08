package br.com.stickerswap.chat.application.dto;

import br.com.stickerswap.chat.domain.model.MessageType;

import java.time.Instant;
import java.util.UUID;

public record MessageResponse(
        UUID messageId,
        UUID conversationId,
        UUID senderUserId,
        MessageType type,
        String body,
        Instant sentAt
) {}
