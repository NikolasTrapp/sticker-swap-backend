package br.com.stickerswap.album.application.dto;

import br.com.stickerswap.album.domain.model.Sticker;

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
    public static StickerResponse from(Sticker s) {
        return new StickerResponse(s.getId(), s.getAlbumId(), s.getNumber(),
                s.getName(), s.getDescription(), s.isActive(), s.getCreatedAt(), s.getUpdatedAt());
    }
}
