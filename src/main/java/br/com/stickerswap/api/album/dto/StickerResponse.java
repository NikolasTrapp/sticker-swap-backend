package br.com.stickerswap.api.album.dto;

import br.com.stickerswap.domain.album.model.Sticker;

import java.time.Instant;
import java.util.UUID;

public record StickerResponse(
        UUID id,
        UUID albumId,
        String number,
        String name,
        String description,
        boolean active,
        Instant createdAt,
        Instant updatedAt
) {
}
