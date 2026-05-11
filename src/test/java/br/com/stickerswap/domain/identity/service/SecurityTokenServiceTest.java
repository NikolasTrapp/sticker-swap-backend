package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.domain.identity.model.SecurityToken;
import br.com.stickerswap.domain.identity.model.SecurityTokenType;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.infrastructure.repository.identity.SecurityTokenRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SecurityTokenServiceTest {

    @Mock SecurityTokenRepository securityTokenRepository;

    @InjectMocks SecurityTokenServiceImpl securityTokenService;

    private final User user = new User();

    // ── createToken ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado usuário válido, quando criar token, então consome tokens abertos existentes e persiste hash distinto do raw token")
    void givenValidUser_whenCreateToken_thenConsumesOpenTokensFirstAndPersistsHash() {
        // Arrange
        when(securityTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, SecurityTokenType.EMAIL_CONFIRMATION))
                .thenReturn(List.of());
        when(securityTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        String rawToken = securityTokenService.createToken(user, SecurityTokenType.EMAIL_CONFIRMATION, Duration.ofHours(24));

        // Assert
        assertThat(rawToken).isNotBlank();
        ArgumentCaptor<SecurityToken> captor = ArgumentCaptor.forClass(SecurityToken.class);
        verify(securityTokenRepository).save(captor.capture());
        SecurityToken saved = captor.getValue();
        assertThat(saved.getTokenHash()).isNotEqualTo(rawToken);
        assertThat(saved.getTokenHash()).hasSize(64); // SHA-256 hex length
        assertThat(saved.getExpiresAt()).isAfter(LocalDateTime.now());
        verify(securityTokenRepository).findByUserAndTypeAndConsumedAtIsNull(user, SecurityTokenType.EMAIL_CONFIRMATION);
    }

    @Test
    @DisplayName("dado tokens abertos existentes, quando criar token, então consome todos antes de criar o novo")
    void givenOpenTokensExist_whenCreateToken_thenOldOnesAreConsumedBeforeNew() {
        // Arrange
        SecurityToken open1 = tokenNotConsumed();
        SecurityToken open2 = tokenNotConsumed();
        when(securityTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, SecurityTokenType.PASSWORD_RESET))
                .thenReturn(List.of(open1, open2));
        when(securityTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        securityTokenService.createToken(user, SecurityTokenType.PASSWORD_RESET, Duration.ofMinutes(30));

        // Assert
        assertThat(open1.getConsumedAt()).isNotNull();
        assertThat(open2.getConsumedAt()).isNotNull();
    }

    // ── consumeToken ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado token válido, quando consumir, então marca consumedAt e retorna token")
    void givenValidRawToken_whenConsumeToken_thenMarksConsumedAtAndReturns() {
        // Arrange
        SecurityToken token = tokenNotConsumed();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(securityTokenRepository.findByTokenHashAndType(any(), eq(SecurityTokenType.EMAIL_CONFIRMATION)))
                .thenReturn(Optional.of(token));
        when(securityTokenRepository.save(token)).thenReturn(token);

        // Act
        SecurityToken result = securityTokenService.consumeToken("raw-token", SecurityTokenType.EMAIL_CONFIRMATION);

        // Assert
        assertThat(result.getConsumedAt()).isNotNull();
        verify(securityTokenRepository).save(token);
    }

    @Test
    @DisplayName("dado token já consumido, quando consumir novamente, então lança BusinessRuleException")
    void givenAlreadyConsumedToken_whenConsumeToken_thenThrowsBusinessRule() {
        // Arrange
        SecurityToken token = new SecurityToken();
        token.setConsumedAt(LocalDateTime.now().minusMinutes(5));
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        when(securityTokenRepository.findByTokenHashAndType(any(), eq(SecurityTokenType.EMAIL_CONFIRMATION)))
                .thenReturn(Optional.of(token));

        // Act & Assert
        assertThatThrownBy(() -> securityTokenService.consumeToken("raw-token", SecurityTokenType.EMAIL_CONFIRMATION))
                .isInstanceOf(BusinessRuleException.class);
        verify(securityTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("dado token expirado, quando consumir, então lança BusinessRuleException")
    void givenExpiredToken_whenConsumeToken_thenThrowsBusinessRule() {
        // Arrange
        SecurityToken token = tokenNotConsumed();
        token.setExpiresAt(LocalDateTime.now().minusSeconds(1));
        when(securityTokenRepository.findByTokenHashAndType(any(), eq(SecurityTokenType.EMAIL_CONFIRMATION)))
                .thenReturn(Optional.of(token));

        // Act & Assert
        assertThatThrownBy(() -> securityTokenService.consumeToken("raw-token", SecurityTokenType.EMAIL_CONFIRMATION))
                .isInstanceOf(BusinessRuleException.class);
        verify(securityTokenRepository, never()).save(any());
    }

    @Test
    @DisplayName("dado hash desconhecido, quando consumir, então lança BusinessRuleException")
    void givenUnknownHash_whenConsumeToken_thenThrowsBusinessRule() {
        // Arrange
        when(securityTokenRepository.findByTokenHashAndType(any(), any())).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> securityTokenService.consumeToken("wrong-token", SecurityTokenType.EMAIL_CONFIRMATION))
                .isInstanceOf(BusinessRuleException.class);
    }

    // ── consumeOpenTokens ─────────────────────────────────────────────────────

    @Test
    @DisplayName("dados tokens abertos existentes, quando consumir todos, então todos recebem consumedAt")
    void givenOpenTokensExist_whenConsumeOpenTokens_thenAllMarkedConsumed() {
        // Arrange
        SecurityToken t1 = tokenNotConsumed();
        SecurityToken t2 = tokenNotConsumed();
        when(securityTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, SecurityTokenType.EMAIL_CONFIRMATION))
                .thenReturn(List.of(t1, t2));
        when(securityTokenRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        securityTokenService.consumeOpenTokens(user, SecurityTokenType.EMAIL_CONFIRMATION);

        // Assert
        assertThat(t1.getConsumedAt()).isNotNull();
        assertThat(t2.getConsumedAt()).isNotNull();
        verify(securityTokenRepository, times(2)).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private SecurityToken tokenNotConsumed() {
        SecurityToken token = new SecurityToken();
        token.setExpiresAt(LocalDateTime.now().plusHours(1));
        return token;
    }
}
