package br.com.stickerswap.api.album.dto;

import br.com.stickerswap.domain.album.model.Album;

import java.time.Instant;
import java.util.UUID;

public record AlbumResponse(
        UUID id,
        String name,
        String description,
        Integer year,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
