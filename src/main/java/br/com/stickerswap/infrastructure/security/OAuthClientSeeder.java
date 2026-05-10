package br.com.stickerswap.infrastructure.security;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.oauth2.core.AuthorizationGrantType;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.oidc.OidcScopes;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;
import org.springframework.security.oauth2.server.authorization.settings.ClientSettings;
import org.springframework.security.oauth2.server.authorization.settings.TokenSettings;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Arrays;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuthClientSeeder implements ApplicationRunner {

    private final RegisteredClientRepository registeredClientRepository;

    @Value("${app.oauth.web-client.client-id:sticker-swap-web}")
    private String clientId;

    @Value("${app.oauth.web-client.redirect-uris:http://localhost:4200/oauth/callback,http://127.0.0.1:4200/oauth/callback}")
    private String redirectUris;

    @Value("${app.oauth.web-client.post-logout-redirect-uris:http://localhost:4200,http://127.0.0.1:4200}")
    private String postLogoutRedirectUris;

    @Override
    public void run(ApplicationArguments args) {
        Set<String> configuredRedirectUris = new LinkedHashSet<>(splitCsv(redirectUris));
        Set<String> configuredPostLogoutUris = new LinkedHashSet<>(splitCsv(postLogoutRedirectUris));

        RegisteredClient existing = registeredClientRepository.findByClientId(clientId);

        if (existing != null) {
            if (existing.getRedirectUris().equals(configuredRedirectUris)
                    && existing.getPostLogoutRedirectUris().equals(configuredPostLogoutUris)) {
                return;
            }
            registeredClientRepository.save(
                    RegisteredClient.from(existing)
                            .redirectUris(uris -> { uris.clear(); uris.addAll(configuredRedirectUris); })
                            .postLogoutRedirectUris(uris -> { uris.clear(); uris.addAll(configuredPostLogoutUris); })
                            .build());
            return;
        }

        RegisteredClient.Builder builder = RegisteredClient.withId(UUID.randomUUID().toString())
                .clientId(clientId)
                .clientName("Sticker Swap Web")
                .clientAuthenticationMethod(ClientAuthenticationMethod.NONE)
                .authorizationGrantType(AuthorizationGrantType.AUTHORIZATION_CODE)
                .authorizationGrantType(AuthorizationGrantType.REFRESH_TOKEN)
                .scope(OidcScopes.OPENID)
                .scope(OidcScopes.PROFILE)
                .scope("api")
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build());

        configuredRedirectUris.forEach(builder::redirectUri);
        configuredPostLogoutUris.forEach(builder::postLogoutRedirectUri);
        registeredClientRepository.save(builder.build());
    }

    private List<String> splitCsv(String value) {
        return Arrays.stream(value.split(","))
                .map(String::trim)
                .filter(item -> !item.isEmpty())
                .toList();
    }
}
