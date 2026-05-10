package br.com.stickerswap.infrastructure.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.OAuthFlow;
import io.swagger.v3.oas.annotations.security.OAuthFlows;
import io.swagger.v3.oas.annotations.security.OAuthScope;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import io.swagger.v3.oas.annotations.security.SecuritySchemes;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(
        title = "Sticker Swap API",
        version = "1.0",
        description = "Platform for trading Copa stickers"
))
@SecuritySchemes({
        @SecurityScheme(
                name = "bearerAuth",
                type = SecuritySchemeType.HTTP,
                scheme = "bearer",
                bearerFormat = "JWT"
        ),
        @SecurityScheme(
                name = "oauth2",
                type = SecuritySchemeType.OAUTH2,
                flows = @OAuthFlows(authorizationCode = @OAuthFlow(
                        authorizationUrl = "/oauth2/authorize",
                        tokenUrl = "/oauth2/token",
                        scopes = {
                                @OAuthScope(name = "openid", description = "OpenID Connect"),
                                @OAuthScope(name = "profile", description = "Profile information"),
                                @OAuthScope(name = "api", description = "Sticker Swap API"),
                                @OAuthScope(name = "offline_access", description = "Refresh token access")
                        }
                ))
        )
})
public class OpenApiConfig {}
