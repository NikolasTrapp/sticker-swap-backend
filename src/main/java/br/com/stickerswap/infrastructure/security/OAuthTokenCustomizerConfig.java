package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.identity.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.oauth2.server.authorization.token.JwtEncodingContext;
import org.springframework.security.oauth2.server.authorization.token.OAuth2TokenCustomizer;

@Configuration
@RequiredArgsConstructor
public class OAuthTokenCustomizerConfig {

    private final UserRepository userRepository;

    @Bean
    public OAuth2TokenCustomizer<JwtEncodingContext> jwtTokenCustomizer() {
        return context -> userRepository.findByEmail(context.getPrincipal().getName())
                .ifPresent(user -> context.getClaims()
                        .subject(user.getId().toString())
                        .claim("email", user.getEmail())
                        .claim("role", user.getRole().name()));
    }
}
