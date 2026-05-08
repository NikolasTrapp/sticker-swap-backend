package br.com.stickerswap.shared.security;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

public record AuthenticatedUser(UUID id, String email, String role) {

    public static AuthenticatedUser fromContext() {
        var auth = SecurityContextHolder.getContext().getAuthentication();
        if (auth instanceof JwtAuthenticationToken jwtAuth) {
            var jwt = jwtAuth.getToken();
            return new AuthenticatedUser(
                    UUID.fromString(jwt.getSubject()),
                    jwt.getClaimAsString("email"),
                    jwt.getClaimAsString("role")
            );
        }
        throw new IllegalStateException("No JWT authentication in context");
    }
}
