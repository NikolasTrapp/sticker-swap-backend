package br.com.stickerswap.infrastructure.repository.profile;

import br.com.stickerswap.domain.profile.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserProfileRepository extends JpaRepository<UserProfile, UUID> {
    Optional<UserProfile> findByUserId(UUID userId);
    List<UserProfile> findByUserIdIn(Collection<UUID> userIds);
}
