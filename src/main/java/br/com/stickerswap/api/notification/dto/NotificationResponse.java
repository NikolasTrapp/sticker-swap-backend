package br.com.stickerswap.api.notification.dto;

import br.com.stickerswap.domain.notification.model.NotificationType;

import java.time.LocalDateTime;
import java.util.UUID;

public record NotificationResponse(
        UUID notificationId,
        NotificationType type,
        UUID conversationId,
        UUID actorUserId,
        String actorNickname,
        UUID stickerId,
        String stickerCode,
        String stickerName,
        boolean read,
        LocalDateTime createdAt
) {}
