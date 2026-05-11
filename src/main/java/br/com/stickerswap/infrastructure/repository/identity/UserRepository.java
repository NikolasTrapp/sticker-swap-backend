package br.com.stickerswap.infrastructure.repository.identity;

import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface UserRepository extends JpaRepository<User, UUID> {
    Optional<User> findByEmail(String email);
    boolean existsByEmail(String email);
    boolean existsByIdAndStatusAndEmailVerifiedTrue(UUID id, UserStatus status);
    Page<User> findByEmailContainingIgnoreCase(String email, Pageable pageable);
}
