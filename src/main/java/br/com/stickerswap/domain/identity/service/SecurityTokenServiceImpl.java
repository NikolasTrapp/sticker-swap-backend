package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.domain.identity.model.SecurityToken;
import br.com.stickerswap.domain.identity.model.SecurityTokenType;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.infrastructure.repository.identity.SecurityTokenRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.SecureRandom;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.HexFormat;

@Service
@RequiredArgsConstructor
public class SecurityTokenServiceImpl implements SecurityTokenService {

    private final SecurityTokenRepository securityTokenRepository;
    private final SecureRandom secureRandom = new SecureRandom();

    @Override
    public String createToken(User user, SecurityTokenType type, Duration ttl) {
        consumeOpenTokens(user, type);

        String rawToken = generateRawToken();
        SecurityToken token = new SecurityToken();
        token.setUser(user);
        token.setType(type);
        token.setTokenHash(hash(rawToken));
        token.setExpiresAt(LocalDateTime.now().plus(ttl));
        securityTokenRepository.save(token);
        return rawToken;
    }

    @Override
    public SecurityToken consumeToken(String rawToken, SecurityTokenType type) {
        LocalDateTime now = LocalDateTime.now();
        SecurityToken token = securityTokenRepository.findByTokenHashAndType(hash(rawToken), type)
                .orElseThrow(() -> new BusinessRuleException("Invalid or expired token"));

        if (token.isConsumed() || token.isExpired(now)) {
            throw new BusinessRuleException("Invalid or expired token");
        }

        token.setConsumedAt(now);
        return securityTokenRepository.save(token);
    }

    @Override
    public void consumeOpenTokens(User user, SecurityTokenType type) {
        LocalDateTime now = LocalDateTime.now();
        securityTokenRepository.findByUserAndTypeAndConsumedAtIsNull(user, type)
                .forEach(token -> {
                    token.setConsumedAt(now);
                    securityTokenRepository.save(token);
                });
    }

    private String generateRawToken() {
        byte[] bytes = new byte[32];
        secureRandom.nextBytes(bytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes);
    }

    private String hash(String rawToken) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(rawToken.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception e) {
            throw new IllegalStateException("Unable to hash security token", e);
        }
    }
}
