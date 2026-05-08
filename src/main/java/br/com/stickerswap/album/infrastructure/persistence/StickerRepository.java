package br.com.stickerswap.album.infrastructure.persistence;

import br.com.stickerswap.album.domain.model.Sticker;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface StickerRepository extends JpaRepository<Sticker, UUID> {
    Page<Sticker> findByAlbumId(UUID albumId, Pageable pageable);
    Page<Sticker> findByAlbumIdAndActive(UUID albumId, boolean active, Pageable pageable);
    boolean existsByAlbumIdAndNumber(UUID albumId, String number);
    boolean existsByAlbumIdAndNumberAndIdNot(UUID albumId, String number, UUID id);
    Optional<Sticker> findByIdAndActive(UUID id, boolean active);
}
