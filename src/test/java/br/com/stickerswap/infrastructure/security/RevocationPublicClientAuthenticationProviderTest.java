package br.com.stickerswap.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class RevocationPublicClientAuthenticationProviderTest {

    @Mock RegisteredClientRepository registeredClientRepository;
    @InjectMocks RevocationPublicClientAuthenticationProvider provider;

    @Test
    @DisplayName("dado cliente público PKCE registrado, quando authenticate(), então retorna token autenticado")
    void givenRegisteredPublicClient_whenAuthenticate_thenReturnsAuthenticatedToken() {
        // Arrange
        RevocationClientAuthenticationToken token = new RevocationClientAuthenticationToken("spa-client");

        RegisteredClient registeredClient = mock(RegisteredClient.class);
        when(registeredClient.getClientAuthenticationMethods())
                .thenReturn(Set.of(ClientAuthenticationMethod.NONE));
        when(registeredClientRepository.findByClientId("spa-client")).thenReturn(registeredClient);

        // Act
        Authentication result = provider.authenticate(token);

        // Assert
        assertThat(result).isInstanceOf(OAuth2ClientAuthenticationToken.class);
        assertThat(result.isAuthenticated()).isTrue();
    }

    @Test
    @DisplayName("dado cliente desconhecido, quando authenticate(), então lança OAuth2AuthenticationException")
    void givenUnknownClientId_whenAuthenticate_thenThrowsOAuth2AuthenticationException() {
        // Arrange
        RevocationClientAuthenticationToken token = new RevocationClientAuthenticationToken("unknown-client");
        when(registeredClientRepository.findByClientId("unknown-client")).thenReturn(null);

        // Act / Assert
        assertThatThrownBy(() -> provider.authenticate(token))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    @DisplayName("dado cliente sem método NONE, quando authenticate(), então lança OAuth2AuthenticationException")
    void givenClientWithoutNoneMethod_whenAuthenticate_thenThrowsOAuth2AuthenticationException() {
        // Arrange
        RevocationClientAuthenticationToken token = new RevocationClientAuthenticationToken("confidential-client");

        RegisteredClient registeredClient = mock(RegisteredClient.class);
        when(registeredClient.getClientAuthenticationMethods())
                .thenReturn(Set.of(ClientAuthenticationMethod.CLIENT_SECRET_BASIC));
        when(registeredClientRepository.findByClientId("confidential-client")).thenReturn(registeredClient);

        // Act / Assert
        assertThatThrownBy(() -> provider.authenticate(token))
                .isInstanceOf(OAuth2AuthenticationException.class);
    }

    @Test
    @DisplayName("dado RevocationClientAuthenticationToken, quando supports(), então retorna true")
    void givenRevocationTokenClass_whenSupports_thenReturnsTrue() {
        // Act / Assert
        assertThat(provider.supports(RevocationClientAuthenticationToken.class)).isTrue();
    }

    @Test
    @DisplayName("dado outra classe de autenticação, quando supports(), então retorna false")
    void givenOtherAuthClass_whenSupports_thenReturnsFalse() {
        // Act / Assert
        assertThat(provider.supports(OAuth2ClientAuthenticationToken.class)).isFalse();
    }
}
