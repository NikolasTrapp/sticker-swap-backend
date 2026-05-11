package br.com.stickerswap.shared.security;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class AuthenticatedUserTest {

    @AfterEach
    void clearContext() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("dado JWT válido no contexto, quando fromContext(), então retorna usuário autenticado")
    void givenValidJwtInContext_whenFromContext_thenReturnsAuthenticatedUser() {
        // Arrange
        UUID userId = UUID.randomUUID();
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        when(jwt.getClaimAsString("email")).thenReturn("user@example.com");
        when(jwt.getClaimAsString("role")).thenReturn("USER");

        JwtAuthenticationToken auth = new JwtAuthenticationToken(jwt);
        SecurityContextHolder.getContext().setAuthentication(auth);

        // Act
        AuthenticatedUser result = AuthenticatedUser.fromContext();

        // Assert
        assertThat(result.id()).isEqualTo(userId);
        assertThat(result.email()).isEqualTo("user@example.com");
        assertThat(result.role()).isEqualTo("USER");
    }

    @Test
    @DisplayName("dado autenticação não-JWT no contexto, quando fromContext(), então lança IllegalStateException")
    void givenNonJwtAuthInContext_whenFromContext_thenThrowsIllegalStateException() {
        // Arrange
        SecurityContextHolder.getContext().setAuthentication(null);

        // Act / Assert
        assertThatThrownBy(AuthenticatedUser::fromContext)
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("No JWT authentication");
    }
}
