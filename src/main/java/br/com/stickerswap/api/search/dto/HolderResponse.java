package br.com.stickerswap.api.search.dto;

import java.time.LocalDateTime;
import java.util.UUID;

public record HolderResponse(
        UUID userId,
        String nickname,
        String city,             // null when showCityStatePublicly = false
        String state,            // null when showCityStatePublicly = false
        int quantity,
        boolean isPotentialMatch,
        LocalDateTime lastActivityAt,
        Double distanceKm        // null when either side has no location
) {}
