package br.com.stickerswap.identity.infrastructure.persistence;

import br.com.stickerswap.identity.domain.model.SecurityToken;
import br.com.stickerswap.identity.domain.model.SecurityTokenType;
import br.com.stickerswap.identity.domain.model.User;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface SecurityTokenRepository extends JpaRepository<SecurityToken, UUID> {
    Optional<SecurityToken> findByTokenHashAndType(String tokenHash, SecurityTokenType type);

    List<SecurityToken> findByUserAndTypeAndConsumedAtIsNull(User user, SecurityTokenType type);
}
