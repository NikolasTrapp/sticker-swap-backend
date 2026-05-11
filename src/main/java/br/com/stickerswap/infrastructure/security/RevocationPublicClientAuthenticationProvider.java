package br.com.stickerswap.infrastructure.security;

import org.springframework.security.authentication.AuthenticationProvider;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.AuthenticationException;
import org.springframework.security.oauth2.core.ClientAuthenticationMethod;
import org.springframework.security.oauth2.core.OAuth2AuthenticationException;
import org.springframework.security.oauth2.core.OAuth2Error;
import org.springframework.security.oauth2.core.OAuth2ErrorCodes;
import org.springframework.security.oauth2.server.authorization.authentication.OAuth2ClientAuthenticationToken;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClient;
import org.springframework.security.oauth2.server.authorization.client.RegisteredClientRepository;

/**
 * Authenticates public PKCE clients on the token revocation endpoint without requiring
 * {@code code_verifier}. {@link org.springframework.security.oauth2.server.authorization.authentication.PublicClientAuthenticationProvider}
 * always calls {@code CodeVerifierAuthenticator}, which fails for revocation requests that
 * legitimately carry no PKCE verifier. This provider handles only
 * {@link RevocationClientAuthenticationToken} so normal PKCE flows remain unaffected.
 */
class RevocationPublicClientAuthenticationProvider implements AuthenticationProvider {

    private final RegisteredClientRepository registeredClientRepository;

    RevocationPublicClientAuthenticationProvider(RegisteredClientRepository registeredClientRepository) {
        this.registeredClientRepository = registeredClientRepository;
    }

    @Override
    public Authentication authenticate(Authentication authentication) throws AuthenticationException {
        RevocationClientAuthenticationToken clientAuthentication =
                (RevocationClientAuthenticationToken) authentication;

        String clientId = clientAuthentication.getPrincipal().toString();
        RegisteredClient registeredClient = registeredClientRepository.findByClientId(clientId);

        if (registeredClient == null
                || !registeredClient.getClientAuthenticationMethods().contains(ClientAuthenticationMethod.NONE)) {
            throw new OAuth2AuthenticationException(new OAuth2Error(OAuth2ErrorCodes.INVALID_CLIENT));
        }

        return new OAuth2ClientAuthenticationToken(registeredClient, ClientAuthenticationMethod.NONE, null);
    }

    @Override
    public boolean supports(Class<?> authentication) {
        return RevocationClientAuthenticationToken.class.isAssignableFrom(authentication);
    }
}
