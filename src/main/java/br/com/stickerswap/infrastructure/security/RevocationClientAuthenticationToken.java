package br.com.stickerswap.infrastructure.security;

import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;

import java.util.Collections;

/** Marker token used exclusively by {@link RevocationPublicClientAuthenticationProvider}. */
class RevocationClientAuthenticationToken extends OAuth2ClientAuthenticationToken {

    RevocationClientAuthenticationToken(String clientId) {
        super(clientId, ClientAuthenticationMethod.NONE, null, Collections.emptyMap());
    }
}
