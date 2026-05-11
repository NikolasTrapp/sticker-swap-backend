package br.com.stickerswap.infrastructure.security;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;

import static org.assertj.core.api.Assertions.assertThat;

class RevocationClientAuthenticationTokenTest {

    @Test
    @DisplayName("dado clientId, quando criar token, então autentica com método NONE e clientId correto")
    void givenClientId_whenCreate_thenHoldsClientIdWithNoneMethod() {
        // Arrange / Act
        RevocationClientAuthenticationToken token = new RevocationClientAuthenticationToken("my-client");

        // Assert
        assertThat(token.getPrincipal()).isEqualTo("my-client");
        assertThat(token.getClientAuthenticationMethod()).isEqualTo(ClientAuthenticationMethod.NONE);
        assertThat(token.getCredentials()).isNull();
    }
}
