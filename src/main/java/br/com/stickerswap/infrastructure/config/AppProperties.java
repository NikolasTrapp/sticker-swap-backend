package br.com.stickerswap.infrastructure.config;

import org.springframework.boot.context.properties.ConfigurationProperties;

import java.time.Duration;
import java.util.List;

@ConfigurationProperties(prefix = "app")
public record AppProperties(
        DatabaseProperties database,
        SecurityProperties security,
        AdminProperties admin,
        OAuthProperties oauth,
        MailProperties mail,
        CepProperties cep
) {

    public record DatabaseProperties(
            String url,
            String username,
            String password,
            String driverClassName,
            String poolName,
            int maximumPoolSize,
            int minimumIdle,
            Duration connectionTimeout,
            Duration idleTimeout,
            Duration maxLifetime,
            Duration keepAliveTime
    ) {}

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

    public record MailProperties(String from, String deliveryMode, String apiKey) {}

    public record CepProperties(String apiBaseUrl, Duration connectTimeout, Duration readTimeout) {}
}
