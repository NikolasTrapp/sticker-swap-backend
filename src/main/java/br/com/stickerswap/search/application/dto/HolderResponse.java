package br.com.stickerswap.search.application.dto;

import java.time.Instant;
import java.util.UUID;

public record HolderResponse(
        UUID userId,
        String nickname,
        String city,             // null when showCityStatePublicly = false
        String state,            // null when showCityStatePublicly = false
        int quantity,
        boolean isPotentialMatch,
        Instant lastActivityAt
) {}
