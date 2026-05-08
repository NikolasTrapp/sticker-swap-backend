package br.com.stickerswap.infrastructure.repository.collection;

import br.com.stickerswap.domain.collection.model.UserWantedSticker;
import org.springframework.data.jpa.repository.JpaRepository;

import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface UserWantedStickerRepository extends JpaRepository<UserWantedSticker, UUID> {
    List<UserWantedSticker> findByUserIdAndAlbumId(UUID userId, UUID albumId);
    Optional<UserWantedSticker> findByUserIdAndStickerId(UUID userId, UUID stickerId);
    boolean existsByUserIdAndStickerId(UUID userId, UUID stickerId);

    /** Returns user IDs (from holderIds) that want at least one sticker the searcher has repeated. */
    @Query("SELECT DISTINCT uws.userId FROM UserWantedSticker uws " +
           "WHERE uws.userId IN :holderIds AND uws.stickerId IN :searcherRepeatedIds")
    List<UUID> findHolderIdsWhoWantAnyOf(
            @Param("holderIds") Collection<UUID> holderIds,
            @Param("searcherRepeatedIds") Collection<UUID> searcherRepeatedIds);
}
