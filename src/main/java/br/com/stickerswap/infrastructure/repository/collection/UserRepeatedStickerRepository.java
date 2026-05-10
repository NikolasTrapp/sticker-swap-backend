package br.com.stickerswap.infrastructure.repository.collection;

import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserRepeatedStickerRepository extends JpaRepository<UserRepeatedSticker, UUID> {
    List<UserRepeatedSticker> findByUserIdAndAlbumId(UUID userId, UUID albumId);
    Optional<UserRepeatedSticker> findByUserIdAndStickerId(UUID userId, UUID stickerId);
    boolean existsByUserIdAndStickerId(UUID userId, UUID stickerId);
    // Used by collection listing
    List<UserRepeatedSticker> findByUserIdAndAlbumIdAndQuantityGreaterThan(UUID userId, UUID albumId, int quantity);
    // Used by search module — all users holding a specific sticker with qty > 0
    List<UserRepeatedSticker> findByAlbumIdAndStickerIdAndQuantityGreaterThan(UUID albumId, UUID stickerId, int quantity);
}
