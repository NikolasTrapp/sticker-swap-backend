package br.com.stickerswap.domain.search.model;

import java.math.BigDecimal;
import java.util.Set;
import java.util.UUID;

public record HolderSearchCriteria(
        UUID albumId,
        UUID stickerId,
        Set<UUID> excludedUserIds,
        Set<UUID> searcherRepeatedStickerIds,
        BigDecimal searcherLat,
        BigDecimal searcherLon
) {}
