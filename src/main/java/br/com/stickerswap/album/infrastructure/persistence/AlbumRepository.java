package br.com.stickerswap.album.infrastructure.persistence;

import br.com.stickerswap.album.domain.model.Album;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;
import java.util.UUID;

public interface AlbumRepository extends JpaRepository<Album, UUID> {
    Page<Album> findByActive(boolean active, Pageable pageable);
    Optional<Album> findByIdAndActive(UUID id, boolean active);
}
