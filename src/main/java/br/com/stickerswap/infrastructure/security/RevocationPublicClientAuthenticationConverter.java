package br.com.stickerswap.infrastructure.security;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.security.core.Authentication;
import org.springframework.security.oauth2.core.endpoint.OAuth2ParameterNames;
import org.springframework.security.web.authentication.AuthenticationConverter;

/**
 * Handles token revocation requests from public PKCE clients.
 *
 * The standard PublicClientAuthenticationConverter only activates when code_verifier
 * is present (PKCE token exchange). Revocation requests don't carry code_verifier,
 * so without this converter the client authentication always fails with 401.
 *
 * Falls back to the configured default client_id when the Angular OIDC library
 * omits it from the request body.
 */
class RevocationPublicClientAuthenticationConverter implements AuthenticationConverter {

    private static final String REVOKE_PATH = "/oauth2/revoke";

    private final String defaultClientId;

    RevocationPublicClientAuthenticationConverter(String defaultClientId) {
        this.defaultClientId = defaultClientId;
    }

    @Override
    public Authentication convert(HttpServletRequest request) {
        if (!REVOKE_PATH.equals(request.getServletPath())) {
            return null;
        }
        if (request.getParameter(OAuth2ParameterNames.CLIENT_SECRET) != null) {
            return null; // confidential client — let the standard converter handle it
        }

        String clientId = request.getParameter(OAuth2ParameterNames.CLIENT_ID);
        if (clientId == null) {
            clientId = defaultClientId;
        }

        return new RevocationClientAuthenticationToken(clientId);
    }
}
