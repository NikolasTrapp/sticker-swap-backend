package br.com.stickerswap.infrastructure.repository.album;

import br.com.stickerswap.domain.album.model.Sticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;
import java.util.UUID;

public interface StickerRepository extends JpaRepository<Sticker, UUID> {
    Page<Sticker> findByAlbumId(UUID albumId, Pageable pageable);
    Page<Sticker> findByAlbumIdAndActive(UUID albumId, boolean active, Pageable pageable);

    @Query("""
            SELECT s
            FROM Sticker s
            WHERE s.albumId = :albumId
              AND s.active = true
              AND (
                    cast(:q as String) IS NULL
                    OR LOWER(s.code) LIKE LOWER(CONCAT('%', cast(:q as String), '%'))
                    OR LOWER(s.name) LIKE LOWER(CONCAT('%', cast(:q as String), '%'))
                  )
            """)
    Page<Sticker> searchActiveStickers(
            @Param("albumId") UUID albumId,
            @Param("q") String q,
            Pageable pageable
    );

    @Query("""
            SELECT s
            FROM Sticker s
            WHERE s.albumId = :albumId
              AND s.active = true
              AND (
                    cast(:q as String) IS NULL
                    OR LOWER(s.code) LIKE LOWER(CONCAT('%', cast(:q as String), '%'))
                    OR LOWER(s.name) LIKE LOWER(CONCAT('%', cast(:q as String), '%'))
                  )
              AND (
                    :repeatedOnly = false
                    OR EXISTS (
                        SELECT 1
                        FROM UserRepeatedSticker r
                        WHERE r.userId = :userId
                          AND r.stickerId = s.id
                          AND r.quantity > 0
                    )
                  )
              AND (
                    :wantedOnly = false
                    OR EXISTS (
                        SELECT 1
                        FROM UserWantedSticker w
                        WHERE w.userId = :userId
                          AND w.stickerId = s.id
                    )
                  )
              AND (
                    :conflictOnly = false
                    OR (
                        EXISTS (
                            SELECT 1
                            FROM UserRepeatedSticker r
                            WHERE r.userId = :userId
                              AND r.stickerId = s.id
                              AND r.quantity > 0
                        )
                        AND EXISTS (
                            SELECT 1
                            FROM UserWantedSticker w
                            WHERE w.userId = :userId
                              AND w.stickerId = s.id
                        )
                    )
                  )
            """)
    Page<Sticker> searchActiveCollectionStickers(
            @Param("albumId") UUID albumId,
            @Param("userId") UUID userId,
            @Param("q") String q,
            @Param("repeatedOnly") boolean repeatedOnly,
            @Param("wantedOnly") boolean wantedOnly,
            @Param("conflictOnly") boolean conflictOnly,
            Pageable pageable
    );

    boolean existsByAlbumIdAndCode(UUID albumId, String code);
    boolean existsByAlbumIdAndCodeAndIdNot(UUID albumId, String code, UUID id);
    Optional<Sticker> findByIdAndActive(UUID id, boolean active);
}
