package br.com.stickerswap.infrastructure.repository.notification;

import br.com.stickerswap.domain.notification.model.Notification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface NotificationRepository extends JpaRepository<Notification, UUID> {

    List<Notification> findTop30ByRecipientUserIdOrderByCreatedAtDesc(UUID recipientUserId);

    long countByRecipientUserIdAndReadFalse(UUID recipientUserId);

    List<Notification> findByRecipientUserIdAndConversationId(UUID recipientUserId, UUID conversationId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientUserId = :userId")
    void markAllReadByRecipient(@Param("userId") UUID userId);

    @Modifying
    @Query("UPDATE Notification n SET n.read = true WHERE n.recipientUserId = :userId AND n.conversationId = :conversationId")
    void markConversationReadByRecipient(@Param("userId") UUID userId, @Param("conversationId") UUID conversationId);
}
