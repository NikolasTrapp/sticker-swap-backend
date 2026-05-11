package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class ActiveUserFilterTest {

    @Mock UserRepository userRepository;
    @Mock FilterChain filterChain;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    ActiveUserFilter filter;

    @BeforeEach
    void setUp() {
        filter = new ActiveUserFilter(userRepository);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("dado contexto sem autenticação, quando requisição chegar, então continua a cadeia sem consultar usuário")
    void givenNoAuthentication_whenRequest_thenContinuesChainWithoutUserLookup() throws Exception {
        // Arrange
        when(request.getRequestURI()).thenReturn("/api/users");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("dado autenticação não-JWT, quando requisição chegar, então continua a cadeia sem consultar usuário")
    void givenNonJwtAuthentication_whenRequest_thenContinuesChainWithoutUserLookup() throws Exception {
        // Arrange
        var nonJwt = mock(org.springframework.security.core.Authentication.class);
        SecurityContextHolder.getContext().setAuthentication(nonJwt);
        when(request.getRequestURI()).thenReturn("/api/users");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("dado JWT e caminho público (/auth/), quando requisição chegar, então continua cadeia sem verificar status do usuário")
    void givenJwtAuthOnPublicAuthPath_whenRequest_thenContinuesChainWithoutUserStatusCheck() throws Exception {
        // Arrange
        setUpJwtAuth(UUID.randomUUID());
        when(request.getRequestURI()).thenReturn("/auth/register");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("dado JWT e usuário ativo, quando requisição protegida chegar, então continua a cadeia normalmente")
    void givenJwtAuthAndActiveUser_whenProtectedPathRequest_thenContinuesChain() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        setUpJwtAuth(userId);
        when(request.getRequestURI()).thenReturn("/api/stickers");
        when(userRepository.existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE)).thenReturn(true);

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("dado JWT e usuário inativo, quando requisição protegida chegar, então retorna 403 sem continuar cadeia")
    void givenJwtAuthAndInactiveUser_whenProtectedPathRequest_thenReturns403() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        setUpJwtAuth(userId);
        when(request.getRequestURI()).thenReturn("/api/stickers");
        when(userRepository.existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE)).thenReturn(false);
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(403);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("dado JWT em caminho do actuator, quando requisição chegar, então trata como caminho público")
    void givenJwtAuthOnActuatorPath_whenRequest_thenContinuesChainWithoutUserStatusCheck() throws Exception {
        // Arrange
        setUpJwtAuth(UUID.randomUUID());
        when(request.getRequestURI()).thenReturn("/actuator/health");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    @Test
    @DisplayName("dado JWT em caminho WebSocket, quando requisição chegar, então trata como caminho público")
    void givenJwtAuthOnWebSocketPath_whenRequest_thenContinuesChainWithoutUserStatusCheck() throws Exception {
        // Arrange
        setUpJwtAuth(UUID.randomUUID());
        when(request.getRequestURI()).thenReturn("/ws/info");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(filterChain).doFilter(request, response);
        verifyNoInteractions(userRepository);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void setUpJwtAuth(UUID userId) {
        Jwt jwt = mock(Jwt.class);
        when(jwt.getSubject()).thenReturn(userId.toString());
        JwtAuthenticationToken jwtAuth = mock(JwtAuthenticationToken.class);
        when(jwtAuth.isAuthenticated()).thenReturn(true);
        when(jwtAuth.getToken()).thenReturn(jwt);
        SecurityContextHolder.getContext().setAuthentication(jwtAuth);
    }
}
