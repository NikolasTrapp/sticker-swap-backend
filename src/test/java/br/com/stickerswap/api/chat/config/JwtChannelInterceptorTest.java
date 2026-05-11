package br.com.stickerswap.api.chat.config;

import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.infrastructure.security.RateLimiterService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.messaging.Message;
import org.springframework.messaging.MessageChannel;
import org.springframework.messaging.simp.stomp.StompCommand;
import org.springframework.messaging.simp.stomp.StompHeaderAccessor;
import org.springframework.messaging.support.MessageBuilder;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.security.oauth2.jwt.JwtException;

import java.util.HashMap;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class JwtChannelInterceptorTest {

    @Mock JwtDecoder jwtDecoder;
    @Mock RateLimiterService rateLimiterService;
    @Mock UserRepository userRepository;
    @Mock MessageChannel channel;
    @InjectMocks JwtChannelInterceptor interceptor;

    private UUID userId;
    private Jwt jwt;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(jwtDecoder.decode(anyString())).thenReturn(jwt);
        when(userRepository.existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE)).thenReturn(true);
    }

    @Test
    @DisplayName("dado CONNECT com JWT válido e usuário ativo, quando preSend, então seta userId na sessão")
    void givenConnectWithValidJwtAndActiveUser_whenPreSend_thenSetsUserIdInSession() {
        // Arrange
        Message<?> message = buildConnectMessage("Bearer valid-token");

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertThat(result).isNotNull();
        StompHeaderAccessor accessor = StompHeaderAccessor.wrap(result);
        assertThat(accessor.getSessionAttributes()).containsKey("userId");
        assertThat(accessor.getSessionAttributes().get("userId")).isEqualTo(userId);
        verify(rateLimiterService).consume(contains(userId.toString()), eq(20), any());
    }

    @Test
    @DisplayName("dado CONNECT sem cabeçalho Authorization, quando preSend, então lança JwtException")
    void givenConnectWithoutAuthHeader_whenPreSend_thenThrowsJwtException() {
        // Arrange
        Message<?> message = buildConnectMessage(null);

        // Act / Assert
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(JwtException.class)
                .hasMessageContaining("Missing Authorization header");
    }

    @Test
    @DisplayName("dado CONNECT com Authorization sem prefixo Bearer, quando preSend, então lança JwtException")
    void givenConnectWithInvalidAuthFormat_whenPreSend_thenThrowsJwtException() {
        // Arrange
        Message<?> message = buildConnectMessage("Basic dXNlcjpwYXNz");

        // Act / Assert
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(JwtException.class);
    }

    @Test
    @DisplayName("dado CONNECT com JWT válido mas usuário inativo, quando preSend, então lança AccessDeniedException")
    void givenConnectWithValidJwtButInactiveUser_whenPreSend_thenThrowsAccessDeniedException() {
        // Arrange
        when(userRepository.existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE)).thenReturn(false);
        Message<?> message = buildConnectMessage("Bearer valid-token");

        // Act / Assert
        assertThatThrownBy(() -> interceptor.preSend(message, channel))
                .isInstanceOf(AccessDeniedException.class);
    }

    @Test
    @DisplayName("dado SEND com userId na sessão e usuário ativo, quando preSend, então passa mensagem adiante")
    void givenSendCommandWithSessionUserId_whenPreSend_thenPassesThrough() {
        // Arrange
        Message<?> message = buildSendMessage(userId, StompCommand.SEND);

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("dado SUBSCRIBE com userId ativo na sessão, quando preSend, então verifica usuário ativo")
    void givenSubscribeCommandWithActiveUser_whenPreSend_thenVerifiesActiveUser() {
        // Arrange
        Message<?> message = buildSendMessage(userId, StompCommand.SUBSCRIBE);

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository).existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE);
    }

    @Test
    @DisplayName("dado SEND sem userId na sessão, quando preSend, então passa mensagem sem verificar usuário")
    void givenSendCommandWithoutSessionUserId_whenPreSend_thenSkipsUserCheck() {
        // Arrange
        Message<?> message = buildSendMessage(null, StompCommand.SEND);

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertThat(result).isNotNull();
        verify(userRepository, never()).existsByIdAndStatusAndEmailVerifiedTrue(any(), any());
    }

    @Test
    @DisplayName("dado DISCONNECT (comando não mapeado), quando preSend, então passa mensagem sem nenhuma verificação")
    void givenDisconnectCommand_whenPreSend_thenPassesThroughWithoutChecks() {
        // Arrange
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.DISCONNECT);
        accessor.setSessionId("session-1");
        Message<?> message = MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertThat(result).isNotNull();
        verify(jwtDecoder, never()).decode(anyString());
        verify(userRepository, never()).existsByIdAndStatusAndEmailVerifiedTrue(any(), any());
    }

    @Test
    @DisplayName("dado mensagem sem StompHeaderAccessor, quando preSend, então retorna mensagem original sem processamento")
    void givenMessageWithoutStompAccessor_whenPreSend_thenReturnsOriginalMessage() {
        // Arrange — plain message with no STOMP headers; accessor lookup returns null
        Message<?> message = MessageBuilder.withPayload(new byte[0]).build();

        // Act
        Message<?> result = interceptor.preSend(message, channel);

        // Assert
        assertThat(result).isSameAs(message);
        verify(jwtDecoder, never()).decode(anyString());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private Message<?> buildConnectMessage(String authHeader) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(StompCommand.CONNECT);
        accessor.setSessionId("session-1");
        accessor.setSessionAttributes(new HashMap<>());
        if (authHeader != null) {
            accessor.setNativeHeader("Authorization", authHeader);
        }
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }

    private Message<?> buildSendMessage(UUID sessionUserId, StompCommand command) {
        StompHeaderAccessor accessor = StompHeaderAccessor.create(command);
        accessor.setSessionId("session-1");
        HashMap<String, Object> attrs = new HashMap<>();
        if (sessionUserId != null) {
            attrs.put("userId", sessionUserId);
        }
        accessor.setSessionAttributes(attrs);
        accessor.setLeaveMutable(true);
        return MessageBuilder.createMessage(new byte[0], accessor.getMessageHeaders());
    }
}
