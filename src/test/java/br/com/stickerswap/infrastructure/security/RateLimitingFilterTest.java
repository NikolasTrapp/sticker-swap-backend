package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.shared.error.RateLimitExceededException;
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
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.jwt.Jwt;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;

import java.io.PrintWriter;
import java.io.StringWriter;
import java.time.Duration;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitingFilterTest {

    @Mock RateLimiterService rateLimiterService;
    @Mock FilterChain filterChain;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;

    RateLimitingFilter filter;

    @BeforeEach
    void setUp() {
        filter = new RateLimitingFilter(rateLimiterService);
        SecurityContextHolder.clearContext();
    }

    @AfterEach
    void tearDown() {
        SecurityContextHolder.clearContext();
    }

    @Test
    @DisplayName("dado POST /auth/register, quando requisição chegar, então consome bucket de registro por IP")
    void givenPostRegister_whenRequestArrives_thenConsumesRegisterBucket() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/register");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.1");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(eq("ip:register:10.0.0.1"), eq(5), eq(Duration.ofHours(1)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("dado POST /auth/email-confirmations, quando requisição chegar, então consome bucket de confirmação de email")
    void givenPostEmailConfirmation_whenRequestArrives_thenConsumesEmailConfirmationBucket() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/email-confirmations");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.2");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(eq("ip:email-confirmation:10.0.0.2"), eq(10), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("dado POST /auth/password-reset-requests, quando requisição chegar, então consome bucket de reset de senha")
    void givenPostPasswordResetRequest_whenRequestArrives_thenConsumesPasswordResetBucket() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/password-reset-requests");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.3");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(eq("ip:password-reset-request:10.0.0.3"), eq(10), eq(Duration.ofHours(1)));
    }

    @Test
    @DisplayName("dado POST /login, quando requisição chegar, então consome bucket de login por IP")
    void givenPostLogin_whenRequestArrives_thenConsumesLoginBucket() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/login");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.4");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(eq("ip:login:10.0.0.4"), eq(10), eq(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("dado POST /oauth2/token com client_id, quando requisição chegar, então consome bucket com client_id no identificador")
    void givenPostOauthTokenWithClientId_whenRequestArrives_thenConsumesOauthBucketWithClientId() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/oauth2/token");
        when(request.getParameter("client_id")).thenReturn("web-client");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(eq("ip:oauth-token:10.0.0.5:web-client"), eq(30), eq(Duration.ofMinutes(1)));
    }

    @Test
    @DisplayName("dado POST /oauth2/token sem client_id, quando requisição chegar, então usa 'anonymous' no identificador do bucket")
    void givenPostOauthTokenWithoutClientId_whenRequestArrives_thenKeyContainsAnonymous() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/oauth2/token");
        when(request.getParameter("client_id")).thenReturn(null);
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.5");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(eq("ip:oauth-token:10.0.0.5:anonymous"), anyInt(), any());
    }

    @Test
    @DisplayName("dado usuário autenticado via JWT em caminho protegido, quando requisição chegar, então consome bucket de API por usuário")
    void givenAuthenticatedJwtOnProtectedPath_whenRequestArrives_thenConsumesUserApiBucket() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        setUpJwtAuth(userId);
        when(request.getMethod()).thenReturn("GET");
        when(request.getRequestURI()).thenReturn("/api/stickers");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(eq("user:api:" + userId), eq(300), eq(Duration.ofMinutes(1)));
        verify(filterChain).doFilter(request, response);
    }

    @Test
    @DisplayName("dado limite excedido, quando requisição chegar, então retorna 429 com corpo ApiError sem continuar cadeia")
    void givenRateLimitExceeded_whenRequestArrives_thenReturns429WithApiErrorBody() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/register");
        when(request.getHeader("X-Forwarded-For")).thenReturn(null);
        when(request.getRemoteAddr()).thenReturn("10.0.0.9");
        doThrow(new RateLimitExceededException("Rate limit exceeded")).when(rateLimiterService)
                .consume(anyString(), anyInt(), any());
        when(response.getWriter()).thenReturn(new PrintWriter(new StringWriter()));

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(response).setStatus(429);
        verify(filterChain, never()).doFilter(any(), any());
    }

    @Test
    @DisplayName("dado cabeçalho X-Forwarded-For com múltiplos IPs, quando resolver IP do cliente, então usa o primeiro IP da lista")
    void givenXForwardedForHeader_whenResolvingIp_thenUsesFirstIp() throws Exception {
        // Arrange
        when(request.getMethod()).thenReturn("POST");
        when(request.getRequestURI()).thenReturn("/auth/register");
        when(request.getHeader("X-Forwarded-For")).thenReturn("203.0.113.1, 10.0.0.1, 172.16.0.1");

        // Act
        filter.doFilterInternal(request, response, filterChain);

        // Assert
        verify(rateLimiterService).consume(startsWith("ip:register:203.0.113.1"), anyInt(), any());
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
