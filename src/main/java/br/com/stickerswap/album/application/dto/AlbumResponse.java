package br.com.stickerswap.album.application.dto;

import br.com.stickerswap.album.domain.model.Album;

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
    public static AlbumResponse from(Album a) {
        return new AlbumResponse(a.getId(), a.getName(), a.getDescription(),
                a.getYear(), a.isActive(), a.getCreatedAt(), a.getUpdatedAt());
    }
}
