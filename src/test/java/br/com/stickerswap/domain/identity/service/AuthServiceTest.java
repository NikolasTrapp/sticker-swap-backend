package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.api.identity.dto.RegisterRequest;
import br.com.stickerswap.domain.identity.model.SecurityToken;
import br.com.stickerswap.domain.identity.model.SecurityTokenType;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.shared.error.EmailAlreadyExistsException;
import br.com.stickerswap.infrastructure.security.RateLimiterService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AuthServiceTest {

    @Mock UserRepository userRepository;
    @Mock PasswordEncoder passwordEncoder;
    @Mock SecurityTokenService securityTokenService;
    @Mock AccountEmailService accountEmailService;
    @Mock RateLimiterService rateLimiterService;
    @Mock JdbcTemplate jdbcTemplate;

    @InjectMocks AuthServiceImpl authService;

    @Test
    void register_createsUnverifiedUserAndSendsConfirmationEmail() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(false);
        when(passwordEncoder.encode("secret123")).thenReturn("encoded");
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(securityTokenService.createToken(any(), eq(SecurityTokenType.EMAIL_CONFIRMATION), any(Duration.class)))
                .thenReturn("raw-token");

        User user = authService.register(new RegisterRequest(" USER@example.com ", "secret123"));

        assertThat(user.getEmail()).isEqualTo("user@example.com");
        assertThat(user.getPasswordHash()).isEqualTo("encoded");
        assertThat(user.getRole()).isEqualTo(UserRole.USER);
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
        assertThat(user.isEmailVerified()).isFalse();
        verify(accountEmailService).sendEmailConfirmation("user@example.com", "raw-token");
    }

    @Test
    void register_throwsConflict_whenEmailAlreadyExists() {
        when(userRepository.existsByEmail("user@example.com")).thenReturn(true);

        assertThatThrownBy(() -> authService.register(new RegisterRequest("user@example.com", "secret123")))
                .isInstanceOf(EmailAlreadyExistsException.class);

        verify(userRepository, never()).save(any());
        verifyNoInteractions(accountEmailService);
    }

    @Test
    void resendEmailConfirmation_isNoopForUnknownEmailButStillRateLimited() {
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        authService.resendEmailConfirmation("missing@example.com");

        verify(rateLimiterService).consume(eq("email:confirmation:missing@example.com"), eq(3), any(Duration.class));
        verifyNoInteractions(accountEmailService);
    }

    @Test
    void confirmEmail_marksUserVerified() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);

        SecurityToken token = new SecurityToken();
        token.setUser(user);
        when(securityTokenService.consumeToken("raw-token", SecurityTokenType.EMAIL_CONFIRMATION)).thenReturn(token);

        User confirmed = authService.confirmEmail("raw-token");

        assertThat(confirmed.isEmailVerified()).isTrue();
        assertThat(confirmed.getEmailVerifiedAt()).isNotNull();
        verify(userRepository).save(user);
    }

    @Test
    void requestPasswordReset_sendsEmailOnlyWhenUserExists() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(securityTokenService.createToken(any(), eq(SecurityTokenType.PASSWORD_RESET), any(Duration.class)))
                .thenReturn("reset-token");

        authService.requestPasswordReset("user@example.com");

        verify(rateLimiterService).consume(eq("email:password-reset:user@example.com"), eq(3), any(Duration.class));
        verify(accountEmailService).sendPasswordReset("user@example.com", "reset-token");
    }

    @Test
    void resetPassword_updatesPasswordAndRevokesAuthorizations() {
        User user = new User();
        user.setEmail("user@example.com");
        user.setStatus(UserStatus.ACTIVE);
        SecurityToken token = new SecurityToken();
        token.setUser(user);
        when(securityTokenService.consumeToken("reset-token", SecurityTokenType.PASSWORD_RESET)).thenReturn(token);
        when(passwordEncoder.encode("new-secret")).thenReturn("encoded-new");

        authService.resetPassword("reset-token", "new-secret");

        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getPasswordHash()).isEqualTo("encoded-new");
        verify(securityTokenService).consumeOpenTokens(user, SecurityTokenType.PASSWORD_RESET);
        verify(jdbcTemplate).update("DELETE FROM oauth2_authorization WHERE principal_name = ?", "user@example.com");
    }
}
