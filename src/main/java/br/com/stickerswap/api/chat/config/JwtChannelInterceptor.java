package br.com.stickerswap.api.chat.config;

import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.infrastructure.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.ChannelInterceptor;
import org.springframework.messaging.support.MessageHeaderAccessor;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.List;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class JwtChannelInterceptor implements ChannelInterceptor {

    private final JwtDecoder jwtDecoder;
    private final RateLimiterService rateLimiterService;
    private final UserRepository userRepository;

    @Override
    public Message<?> preSend(Message<?> message, MessageChannel channel) {
        StompHeaderAccessor accessor =
                MessageHeaderAccessor.getAccessor(message, StompHeaderAccessor.class);

        if (accessor == null) {
            return message;
        }

        if (StompCommand.CONNECT.equals(accessor.getCommand())) {
            String authHeader = accessor.getFirstNativeHeader("Authorization");
            if (authHeader == null || !authHeader.startsWith("Bearer ")) {
                throw new JwtException("Missing Authorization header on STOMP CONNECT");
            }
            Jwt jwt = jwtDecoder.decode(authHeader.substring(7));
            UUID userId = UUID.fromString(jwt.getSubject());
            requireActiveUser(userId);
            rateLimiterService.consume("user:ws-connect:" + userId, 20, Duration.ofMinutes(1));
            var sessionAttributes = accessor.getSessionAttributes();
            if (sessionAttributes == null) {
                throw new JwtException("Missing STOMP session attributes");
            }
            sessionAttributes.put("userId", userId);
            accessor.setUser(new UsernamePasswordAuthenticationToken(userId.toString(), null, List.of()));
        } else if (StompCommand.SEND.equals(accessor.getCommand()) || StompCommand.SUBSCRIBE.equals(accessor.getCommand())) {
            var sessionAttributes = accessor.getSessionAttributes();
            UUID userId = sessionAttributes != null ? (UUID) sessionAttributes.get("userId") : null;
            if (userId != null) {
                requireActiveUser(userId);
            }
        }

        return message;
    }

    private void requireActiveUser(UUID userId) {
        boolean active = userRepository.existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE);
        if (!active) {
            throw new AccessDeniedException("Account is blocked or inactive");
        }
    }
}
