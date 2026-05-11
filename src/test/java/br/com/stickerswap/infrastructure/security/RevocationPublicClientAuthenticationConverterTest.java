package br.com.stickerswap.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.Authentication;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class RevocationPublicClientAuthenticationConverterTest {

    private static final String DEFAULT_CLIENT_ID = "spa-client";

    private final RevocationPublicClientAuthenticationConverter converter =
            new RevocationPublicClientAuthenticationConverter(DEFAULT_CLIENT_ID);

    @Test
    @DisplayName("dado requisição para /oauth2/revoke sem client_secret, quando convert(), então retorna token de revogação")
    void givenRevokePathWithoutClientSecret_whenConvert_thenReturnsRevocationToken() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/oauth2/revoke");
        when(request.getParameter("client_secret")).thenReturn(null);
        when(request.getParameter("client_id")).thenReturn("my-client");

        // Act
        Authentication result = converter.convert(request);

        // Assert
        assertThat(result).isInstanceOf(RevocationClientAuthenticationToken.class);
        assertThat(result.getPrincipal()).isEqualTo("my-client");
    }

    @Test
    @DisplayName("dado requisição para outro path, quando convert(), então retorna null")
    void givenNonRevokePath_whenConvert_thenReturnsNull() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/oauth2/token");

        // Act
        Authentication result = converter.convert(request);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("dado requisição com client_secret presente, quando convert(), então retorna null (deixa conversor padrão processar)")
    void givenRevokePathWithClientSecret_whenConvert_thenReturnsNull() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/oauth2/revoke");
        when(request.getParameter("client_secret")).thenReturn("secret-value");

        // Act
        Authentication result = converter.convert(request);

        // Assert
        assertThat(result).isNull();
    }

    @Test
    @DisplayName("dado requisição sem client_id no body, quando convert(), então usa client_id padrão configurado")
    void givenRevokePathWithoutClientId_whenConvert_thenUsesDefaultClientId() {
        // Arrange
        HttpServletRequest request = mock(HttpServletRequest.class);
        when(request.getServletPath()).thenReturn("/oauth2/revoke");
        when(request.getParameter("client_secret")).thenReturn(null);
        when(request.getParameter("client_id")).thenReturn(null);

        // Act
        Authentication result = converter.convert(request);

        // Assert
        assertThat(result).isInstanceOf(RevocationClientAuthenticationToken.class);
        assertThat(result.getPrincipal()).isEqualTo(DEFAULT_CLIENT_ID);
    }
}
