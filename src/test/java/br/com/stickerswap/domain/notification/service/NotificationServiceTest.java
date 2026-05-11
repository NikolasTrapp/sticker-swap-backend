package br.com.stickerswap.domain.notification.service;

import br.com.stickerswap.api.notification.dto.NotificationResponse;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.notification.event.NotificationCreatedEvent;
import br.com.stickerswap.domain.notification.model.Notification;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.infrastructure.repository.notification.NotificationRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class NotificationServiceTest {

    @Mock private NotificationRepository notificationRepo;
    @Mock private UserProfileRepository profileRepo;
    @Mock private StickerRepository stickerRepo;
    @Mock private ApplicationEventPublisher eventPublisher;
    @Mock private SimpMessagingTemplate messagingTemplate;

    @InjectMocks private NotificationServiceImpl notificationService;

    private final UUID recipientId = UUID.randomUUID();
    private final UUID actorId = UUID.randomUUID();
    private final UUID conversationId = UUID.randomUUID();
    private final UUID stickerId = UUID.randomUUID();

    @Test
    void notifyInterest_SavesNotificationAndPublishesEvent() {
        UserProfile actorProfile = new UserProfile();
        actorProfile.setNickname("Actor");
        Sticker sticker = new Sticker();
        sticker.setCode("001");
        
        when(profileRepo.findByUserId(actorId)).thenReturn(Optional.of(actorProfile));
        when(stickerRepo.findById(stickerId)).thenReturn(Optional.of(sticker));

        notificationService.notifyInterest(recipientId, actorId, conversationId, stickerId);

        verify(notificationRepo).save(any(Notification.class));
        verify(eventPublisher).publishEvent(any(NotificationCreatedEvent.class));
    }

    @Test
    void markAsRead_WhenExistsAndRecipientMatches_SetsReadTrue() {
        Notification n = new Notification();
        n.setRecipientUserId(recipientId);
        n.setRead(false);
        UUID nId = UUID.randomUUID();

        when(notificationRepo.findById(nId)).thenReturn(Optional.of(n));

        notificationService.markAsRead(recipientId, nId);

        assertThat(n.isRead()).isTrue();
        verify(notificationRepo).save(n);
    }

    @Test
    void markAsRead_WhenRecipientMismatch_ThrowsException() {
        Notification n = new Notification();
        n.setRecipientUserId(UUID.randomUUID());
        UUID nId = UUID.randomUUID();

        when(notificationRepo.findById(nId)).thenReturn(Optional.of(n));

        assertThatThrownBy(() -> notificationService.markAsRead(recipientId, nId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void pushNotification_SendsMessageViaWebSocket() {
        NotificationResponse dto = mock(NotificationResponse.class);
        NotificationCreatedEvent event = new NotificationCreatedEvent(recipientId, dto);

        notificationService.pushNotification(event);

        verify(messagingTemplate).convertAndSendToUser(
                eq(recipientId.toString()),
                eq("/queue/notifications"),
                eq(dto)
        );
    }

    @Test
    void listRecent_ReturnsMappedDtos() {
        Notification n = new Notification();
        n.setActorUserId(actorId);
        n.setStickerId(stickerId);

        UserProfile actorProfile = new UserProfile();
        actorProfile.setUserId(actorId);
        actorProfile.setNickname("Actor");

        Sticker sticker = new Sticker();
        sticker.setId(stickerId);
        sticker.setCode("001");

        when(notificationRepo.findTop30ByRecipientUserIdOrderByCreatedAtDesc(recipientId))
                .thenReturn(List.of(n));
        when(profileRepo.findByUserIdIn(anySet())).thenReturn(List.of(actorProfile));
        when(stickerRepo.findAllById(anySet())).thenReturn(List.of(sticker));

        List<NotificationResponse> result = notificationService.listRecent(recipientId);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).actorNickname()).isEqualTo("Actor");
        assertThat(result.get(0).stickerCode()).isEqualTo("001");
    }

    @Test
    @DisplayName("dado lista vazia, quando listRecent(), então retorna lista vazia sem acessar repositórios de perfil/sticker")
    void givenEmptyNotifications_whenListRecent_thenReturnsEmptyList() {
        // Arrange
        when(notificationRepo.findTop30ByRecipientUserIdOrderByCreatedAtDesc(recipientId))
                .thenReturn(List.of());

        // Act
        List<NotificationResponse> result = notificationService.listRecent(recipientId);

        // Assert
        assertThat(result).isEmpty();
        verify(profileRepo, never()).findByUserIdIn(any());
    }

    @Test
    @DisplayName("dado notificação com actorId e stickerId nulos, quando listRecent(), então trata nulos corretamente")
    void givenNotificationWithNullActorAndSticker_whenListRecent_thenHandlesNullsGracefully() {
        // Arrange
        Notification n = new Notification();
        n.setActorUserId(null);
        n.setStickerId(null);

        when(notificationRepo.findTop30ByRecipientUserIdOrderByCreatedAtDesc(recipientId))
                .thenReturn(List.of(n));
        when(profileRepo.findByUserIdIn(anySet())).thenReturn(List.of());
        when(stickerRepo.findAllById(anySet())).thenReturn(List.of());

        // Act
        List<NotificationResponse> result = notificationService.listRecent(recipientId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.get(0).actorNickname()).isNull();
        assertThat(result.get(0).stickerCode()).isNull();
        assertThat(result.get(0).stickerName()).isNull();
    }

    @Test
    @DisplayName("dado countUnread, quando invocado, então delega ao repositório")
    void givenCountUnread_whenInvoked_thenDelegatesToRepository() {
        // Arrange
        when(notificationRepo.countByRecipientUserIdAndReadFalse(recipientId)).thenReturn(7L);

        // Act
        long count = notificationService.countUnread(recipientId);

        // Assert
        assertThat(count).isEqualTo(7L);
    }
}
