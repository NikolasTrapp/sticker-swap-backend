package br.com.stickerswap.api.collection.dto;

import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.collection.model.UserWantedSticker;

import java.util.UUID;

public record CollectionStickerResponse(
        UUID stickerId,
        String code,
        String name,
        String description,
        int repeatedQuantity,
        boolean wanted,
        String warning
) {
    private static final String OVERLAP_WARNING =
            "Você marcou esta figurinha como repetida e desejada ao mesmo tempo. Confirme se isso é intencional.";

    public static CollectionStickerResponse from(
            Sticker sticker,
            UserRepeatedSticker repeated,
            UserWantedSticker wanted
    ) {
        int quantity = repeated == null ? 0 : repeated.getQuantity();
        boolean hasWanted = wanted != null;
        boolean hasRepeated = quantity > 0;

        return new CollectionStickerResponse(
                sticker.getId(),
                sticker.getCode(),
                sticker.getName(),
                sticker.getDescription(),
                quantity,
                hasWanted,
                hasRepeated && hasWanted ? OVERLAP_WARNING : null
        );
    }
}
