package br.com.stickerswap.domain.identity.event;

import java.util.UUID;

public record UserAccessRevokedEvent(
        UUID userId,
        String reason
) {}
