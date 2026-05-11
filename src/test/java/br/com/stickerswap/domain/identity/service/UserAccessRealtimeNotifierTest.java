package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.api.security.dto.SecurityEventResponse;
import br.com.stickerswap.domain.identity.event.UserAccessRevokedEvent;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class UserAccessRealtimeNotifierTest {

    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks UserAccessRealtimeNotifier notifier;

    @Test
    @DisplayName("dado evento de revogação, quando notificar usuário, então envia mensagem de segurança ao destino correto")
    void givenUserAccessRevokedEvent_whenNotify_thenSendsSecurityMessageToUser() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserAccessRevokedEvent event = new UserAccessRevokedEvent(userId, "BLOCKED_BY_ADMIN");

        // Act
        notifier.notifyUserAccessRevoked(event);

        // Assert
        ArgumentCaptor<SecurityEventResponse> captor = ArgumentCaptor.forClass(SecurityEventResponse.class);
        verify(messagingTemplate).convertAndSendToUser(
                eq(userId.toString()),
                eq("/queue/security"),
                captor.capture()
        );
        assertThat(captor.getValue().type()).isEqualTo("BLOCKED_BY_ADMIN");
        assertThat(captor.getValue().message()).isNotBlank();
    }
}
