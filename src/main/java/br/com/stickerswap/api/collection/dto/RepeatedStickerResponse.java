package br.com.stickerswap.api.collection.dto;

import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;

import java.util.UUID;

public record RepeatedStickerResponse(
        UUID id,
        UUID stickerId,
        String stickerNumber,
        String stickerName,
        int quantity,
        String warning
) {
    private static final String OVERLAP_WARNING =
            "Você marcou esta figurinha como repetida e desejada ao mesmo tempo. Confirme se isso é intencional.";

    public static RepeatedStickerResponse from(UserRepeatedSticker entry, Sticker sticker, boolean alsoWanted) {
        return new RepeatedStickerResponse(
                entry.getId(),
                sticker.getId(),
                sticker.getNumber(),
                sticker.getName(),
                entry.getQuantity(),
                alsoWanted ? OVERLAP_WARNING : null
        );
    }
}
