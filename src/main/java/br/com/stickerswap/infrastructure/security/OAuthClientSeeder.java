package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
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
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class OAuthClientSeeder implements ApplicationRunner {

    private final RegisteredClientRepository registeredClientRepository;
    private final AppProperties appProperties;

    @Override
    public void run(ApplicationArguments args) {
        AppProperties.WebClientProperties wc = appProperties.oauth().webClient();
        Set<String> redirectUris = new LinkedHashSet<>(wc.redirectUris());
        Set<String> postLogoutUris = new LinkedHashSet<>(wc.postLogoutRedirectUris());

        RegisteredClient existing = registeredClientRepository.findByClientId(wc.clientId());

        if (existing != null) {
            registeredClientRepository.save(
                    configureWebClient(RegisteredClient.from(existing), redirectUris, postLogoutUris).build());
            return;
        }

        registeredClientRepository.save(
                configureWebClient(
                        RegisteredClient.withId(UUID.randomUUID().toString()).clientId(wc.clientId()),
                        redirectUris,
                        postLogoutUris
                ).build());
    }

    private RegisteredClient.Builder configureWebClient(
            RegisteredClient.Builder builder,
            Set<String> redirectUris,
            Set<String> postLogoutUris
    ) {
        return builder
                .clientName("Sticker Swap Web")
                .clientAuthenticationMethods(methods -> {
                    methods.clear();
                    methods.add(ClientAuthenticationMethod.NONE);
                })
                .authorizationGrantTypes(grantTypes -> {
                    grantTypes.clear();
                    grantTypes.add(AuthorizationGrantType.AUTHORIZATION_CODE);
                    grantTypes.add(AuthorizationGrantType.REFRESH_TOKEN);
                })
                .redirectUris(uris -> {
                    uris.clear();
                    uris.addAll(redirectUris);
                })
                .postLogoutRedirectUris(uris -> {
                    uris.clear();
                    uris.addAll(postLogoutUris);
                })
                .scopes(scopes -> {
                    scopes.clear();
                    scopes.add(OidcScopes.OPENID);
                    scopes.add(OidcScopes.PROFILE);
                    scopes.add("api");
                    scopes.add("offline_access");
                })
                .clientSettings(ClientSettings.builder()
                        .requireProofKey(true)
                        .requireAuthorizationConsent(false)
                        .build())
                .tokenSettings(TokenSettings.builder()
                        .accessTokenTimeToLive(Duration.ofHours(1))
                        .refreshTokenTimeToLive(Duration.ofDays(30))
                        .reuseRefreshTokens(false)
                        .build());
    }
}
