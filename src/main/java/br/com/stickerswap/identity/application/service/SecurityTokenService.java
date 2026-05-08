package br.com.stickerswap.identity.application.service;

import br.com.stickerswap.identity.domain.model.SecurityToken;
import br.com.stickerswap.identity.domain.model.SecurityTokenType;
import br.com.stickerswap.identity.domain.model.User;

import java.time.Duration;

public interface SecurityTokenService {

    String createToken(User user, SecurityTokenType type, Duration ttl);

    SecurityToken consumeToken(String rawToken, SecurityTokenType type);

    void consumeOpenTokens(User user, SecurityTokenType type);
}
