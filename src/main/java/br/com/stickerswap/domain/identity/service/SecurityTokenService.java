package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.domain.identity.model.SecurityToken;
import br.com.stickerswap.domain.identity.model.SecurityTokenType;
import br.com.stickerswap.domain.identity.model.User;

import java.time.Duration;

public interface SecurityTokenService {

    String createToken(User user, SecurityTokenType type, Duration ttl);

    SecurityToken consumeToken(String rawToken, SecurityTokenType type);

    void consumeOpenTokens(User user, SecurityTokenType type);
}
