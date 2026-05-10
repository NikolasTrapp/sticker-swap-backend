package br.com.stickerswap.api.album.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record AlbumResponse(
        UUID id,
        String name,
        String description,
        Integer year,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
