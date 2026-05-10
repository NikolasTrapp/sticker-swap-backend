package br.com.stickerswap.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        SecurityProperties security,
        AdminProperties admin,
        OAuthProperties oauth,
        MailProperties mail
) {
    public record SecurityProperties(
            String issuer,
            String publicBaseUrl,
            String passwordResetUrl,
            String frontendLoginUrl,
            String jwkSetJson,
            String jwkSetJsonFile,
            CorsProperties cors
    ) {}

    public record CorsProperties(List<String> allowedOrigins) {}

    public record AdminProperties(String email, String password) {}

    public record OAuthProperties(WebClientProperties webClient) {}

    public record WebClientProperties(
            String clientId,
            List<String> redirectUris,
            List<String> postLogoutRedirectUris
    ) {}

    public record MailProperties(String from, String deliveryMode) {}
}
