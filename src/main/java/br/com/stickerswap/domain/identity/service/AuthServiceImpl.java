package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.api.identity.dto.RegisterRequest;
import br.com.stickerswap.domain.identity.model.SecurityTokenType;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.EmailAlreadyExistsException;
import br.com.stickerswap.infrastructure.security.RateLimiterService;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private static final Duration EMAIL_CONFIRMATION_TTL = Duration.ofHours(24);
    private static final Duration PASSWORD_RESET_TTL = Duration.ofMinutes(30);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final SecurityTokenService securityTokenService;
    private final AccountEmailService accountEmailService;
    private final RateLimiterService rateLimiterService;
    private final JdbcTemplate jdbcTemplate;

    @Transactional
    @Override
    public User register(RegisterRequest request) {
        String email = normalizeEmail(request.email());
        if (userRepository.existsByEmail(email)) {
            throw new EmailAlreadyExistsException(email);
        }
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(false);
        User savedUser = userRepository.save(user);

        String token = securityTokenService.createToken(savedUser, SecurityTokenType.EMAIL_CONFIRMATION,
                EMAIL_CONFIRMATION_TTL);
        accountEmailService.sendEmailConfirmation(savedUser.getEmail(), token);
        return savedUser;
    }

    @Transactional
    @Override
    public void resendEmailConfirmation(String email) {
        String normalizedEmail = normalizeEmail(email);
        rateLimiterService.consume("email:confirmation:" + normalizedEmail, 3, Duration.ofHours(1));

        userRepository.findByEmail(normalizedEmail)
                .filter(user -> !user.isEmailVerified())
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    String token = securityTokenService.createToken(user, SecurityTokenType.EMAIL_CONFIRMATION,
                            EMAIL_CONFIRMATION_TTL);
                    accountEmailService.sendEmailConfirmation(user.getEmail(), token);
                });
    }

    @Transactional
    @Override
    public User confirmEmail(String rawToken) {
        var securityToken = securityTokenService.consumeToken(rawToken, SecurityTokenType.EMAIL_CONFIRMATION);
        User user = securityToken.getUser();

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessRuleException("Account is inactive");
        }

        if (!user.isEmailVerified()) {
            user.setEmailVerified(true);
            user.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(user);
        }

        return user;
    }

    @Transactional
    @Override
    public void requestPasswordReset(String email) {
        String normalizedEmail = normalizeEmail(email);
        rateLimiterService.consume("email:password-reset:" + normalizedEmail, 3, Duration.ofHours(1));

        userRepository.findByEmail(normalizedEmail)
                .filter(user -> user.getStatus() == UserStatus.ACTIVE)
                .ifPresent(user -> {
                    String token = securityTokenService.createToken(user, SecurityTokenType.PASSWORD_RESET,
                            PASSWORD_RESET_TTL);
                    accountEmailService.sendPasswordReset(user.getEmail(), token);
                });
    }

    @Transactional
    @Override
    public void resetPassword(String rawToken, String newPassword) {
        var securityToken = securityTokenService.consumeToken(rawToken, SecurityTokenType.PASSWORD_RESET);
        User user = securityToken.getUser();

        if (user.getStatus() == UserStatus.INACTIVE) {
            throw new BusinessRuleException("Account is inactive");
        }

        user.setPasswordHash(passwordEncoder.encode(newPassword));
        userRepository.save(user);
        securityTokenService.consumeOpenTokens(user, SecurityTokenType.PASSWORD_RESET);
        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE principal_name = ?", user.getEmail());
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase();
    }
}
