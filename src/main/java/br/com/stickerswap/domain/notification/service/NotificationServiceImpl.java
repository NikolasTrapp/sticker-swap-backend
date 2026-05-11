package br.com.stickerswap.domain.notification.service;

import br.com.stickerswap.api.notification.dto.NotificationResponse;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.notification.event.NotificationCreatedEvent;
import br.com.stickerswap.domain.notification.model.Notification;
import br.com.stickerswap.domain.notification.model.NotificationType;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.infrastructure.repository.notification.NotificationRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class NotificationServiceImpl implements NotificationService {

    private final NotificationRepository notificationRepo;
    private final UserProfileRepository profileRepo;
    private final StickerRepository stickerRepo;
    private final ApplicationEventPublisher eventPublisher;
    private final SimpMessagingTemplate messagingTemplate;

    @Transactional
    @Override
    public void notifyInterest(UUID holderId, UUID seekerId, UUID conversationId, UUID stickerId) {
        Notification n = new Notification();
        n.setRecipientUserId(holderId);
        n.setType(NotificationType.NEW_INTEREST);
        n.setConversationId(conversationId);
        n.setActorUserId(seekerId);
        n.setStickerId(stickerId);
        notificationRepo.save(n);

        NotificationResponse dto = resolveDto(n, seekerId, stickerId);
        eventPublisher.publishEvent(new NotificationCreatedEvent(holderId, dto));
    }

    @Transactional
    @Override
    public void notifyMessage(UUID recipientId, UUID senderId, UUID conversationId, UUID stickerId) {
        Notification n = new Notification();
        n.setRecipientUserId(recipientId);
        n.setType(NotificationType.NEW_MESSAGE);
        n.setConversationId(conversationId);
        n.setActorUserId(senderId);
        n.setStickerId(stickerId);
        notificationRepo.save(n);

        NotificationResponse dto = resolveDto(n, senderId, stickerId);
        eventPublisher.publishEvent(new NotificationCreatedEvent(recipientId, dto));
    }

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void pushNotification(NotificationCreatedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.recipientUserId().toString(),
                "/queue/notifications",
                event.dto()
        );
    }

    @Transactional
    @Override
    public void markConversationRead(UUID userId, UUID conversationId) {
        notificationRepo.markConversationReadByRecipient(userId, conversationId);
    }

    @Transactional
    @Override
    public void markAsRead(UUID userId, UUID notificationId) {
        Notification n = notificationRepo.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", notificationId));
        if (!n.getRecipientUserId().equals(userId)) {
            throw new ResourceNotFoundException("Notification", notificationId);
        }
        n.setRead(true);
        notificationRepo.save(n);
    }

    @Transactional
    @Override
    public void markAllAsRead(UUID userId) {
        notificationRepo.markAllReadByRecipient(userId);
    }

    @Transactional(readOnly = true)
    @Override
    public List<NotificationResponse> listRecent(UUID userId) {
        List<Notification> notifications = notificationRepo
                .findTop30ByRecipientUserIdOrderByCreatedAtDesc(userId);
        if (notifications.isEmpty()) return List.of();

        Set<UUID> actorIds = notifications.stream()
                .map(Notification::getActorUserId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        Set<UUID> stickerIds = notifications.stream()
                .map(Notification::getStickerId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());

        Map<UUID, String> nicknames = new HashMap<>();
        profileRepo.findByUserIdIn(actorIds)
                .forEach(profile -> nicknames.put(profile.getUserId(), profile.getNickname()));
        Map<UUID, Sticker> stickers = stickerRepo.findAllById(stickerIds).stream()
                .collect(Collectors.toMap(Sticker::getId, s -> s));

        return notifications.stream().map(n -> new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getConversationId(),
                n.getActorUserId(),
                n.getActorUserId() != null ? nicknames.get(n.getActorUserId()) : null,
                n.getStickerId(),
                n.getStickerId() != null && stickers.containsKey(n.getStickerId())
                        ? stickers.get(n.getStickerId()).getCode() : null,
                n.getStickerId() != null && stickers.containsKey(n.getStickerId())
                        ? stickers.get(n.getStickerId()).getName() : null,
                n.isRead(),
                n.getCreatedAt()
        )).toList();
    }

    @Transactional(readOnly = true)
    @Override
    public long countUnread(UUID userId) {
        return notificationRepo.countByRecipientUserIdAndReadFalse(userId);
    }

    private NotificationResponse resolveDto(Notification n, UUID actorUserId, UUID stickerId) {
        String nickname = profileRepo.findByUserId(actorUserId)
                .map(UserProfile::getNickname).orElse(null);
        Sticker sticker = stickerId != null
                ? stickerRepo.findById(stickerId).orElse(null) : null;
        return new NotificationResponse(
                n.getId(),
                n.getType(),
                n.getConversationId(),
                actorUserId,
                nickname,
                stickerId,
                sticker != null ? sticker.getCode() : null,
                sticker != null ? sticker.getName() : null,
                false,
                n.getCreatedAt()
        );
    }
}
