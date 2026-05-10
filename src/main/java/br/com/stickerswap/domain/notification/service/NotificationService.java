package br.com.stickerswap.domain.notification.service;

import br.com.stickerswap.api.notification.dto.NotificationResponse;

import java.util.List;
import java.util.UUID;

public interface NotificationService {

    void notifyInterest(UUID holderId, UUID seekerId, UUID conversationId, UUID stickerId);

    void notifyMessage(UUID recipientId, UUID senderId, UUID conversationId, UUID stickerId);

    void markConversationRead(UUID userId, UUID conversationId);

    void markAsRead(UUID userId, UUID notificationId);

    void markAllAsRead(UUID userId);

    List<NotificationResponse> listRecent(UUID userId);

    long countUnread(UUID userId);
}
