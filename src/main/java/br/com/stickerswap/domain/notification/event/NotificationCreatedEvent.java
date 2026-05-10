package br.com.stickerswap.domain.notification.event;

import br.com.stickerswap.api.notification.dto.NotificationResponse;

import java.util.UUID;

public record NotificationCreatedEvent(UUID recipientUserId, NotificationResponse dto) {}
