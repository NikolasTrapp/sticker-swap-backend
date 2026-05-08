package br.com.stickerswap.collection.application.dto;

import br.com.stickerswap.album.domain.model.Sticker;
import br.com.stickerswap.collection.domain.model.UserWantedSticker;

import java.util.UUID;

public record WantedStickerResponse(
        UUID id,
        UUID stickerId,
        String stickerNumber,
        String stickerName,
        String warning
) {
    private static final String OVERLAP_WARNING =
            "Você marcou esta figurinha como repetida e desejada ao mesmo tempo. Confirme se isso é intencional.";

    public static WantedStickerResponse from(UserWantedSticker entry, Sticker sticker, boolean alsoRepeated) {
        return new WantedStickerResponse(
                entry.getId(),
                sticker.getId(),
                sticker.getNumber(),
                sticker.getName(),
                alsoRepeated ? OVERLAP_WARNING : null
        );
    }
}
