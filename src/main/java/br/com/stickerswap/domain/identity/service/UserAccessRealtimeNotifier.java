package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.api.security.dto.SecurityEventResponse;
import br.com.stickerswap.domain.identity.event.UserAccessRevokedEvent;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@Component
@RequiredArgsConstructor
public class UserAccessRealtimeNotifier {

    private final SimpMessagingTemplate messagingTemplate;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void notifyUserAccessRevoked(UserAccessRevokedEvent event) {
        messagingTemplate.convertAndSendToUser(
                event.userId().toString(),
                "/queue/security",
                new SecurityEventResponse(event.reason(), "Sua conta foi bloqueada por um administrador.")
        );
    }
}
