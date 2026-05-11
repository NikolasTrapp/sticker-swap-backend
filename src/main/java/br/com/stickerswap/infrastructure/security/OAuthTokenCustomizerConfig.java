package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;
import org.springframework.web.context.request.RequestContextHolder;
import org.springframework.web.context.request.ServletRequestAttributes;

import java.time.LocalDateTime;
import java.util.Optional;

@Configuration
@RequiredArgsConstructor
public class OAuthTokenCustomizerConfig {

    private final UserRepository userRepository;

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> userRepository.findByEmail(context.getPrincipal().getName())
                .ifPresent(user -> {
                    context.getClaims()
                            .subject(user.getId().toString())
                            .claim("email", user.getEmail())
                            .claim("role", user.getRole().name());

                    user.setLastActivityAt(LocalDateTime.now());
                    user.setLastIpAddress(resolveClientIp());
                    userRepository.save(user);
                });
    }

    private String resolveClientIp() {
        try {
            HttpServletRequest request = ((ServletRequestAttributes) RequestContextHolder.currentRequestAttributes()).getRequest();
            return Optional.ofNullable(request.getHeader("X-Forwarded-For"))
                    .filter(h -> !h.isBlank())
                    .map(h -> h.split(",")[0].trim())
                    .orElse(request.getRemoteAddr());
        } catch (IllegalStateException e) {
            return null;
        }
    }
}
