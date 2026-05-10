package br.com.stickerswap.api.album.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record StickerResponse(
        UUID id,
        UUID albumId,
        String code,
        String name,
        String description,
        boolean active,
        LocalDateTime createdAt,
        LocalDateTime updatedAt
) {
}
