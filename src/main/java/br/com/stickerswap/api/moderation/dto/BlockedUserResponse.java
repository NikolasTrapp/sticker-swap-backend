package br.com.stickerswap.api.moderation.dto;

import br.com.stickerswap.domain.moderation.model.UserBlock;
import br.com.stickerswap.domain.profile.model.UserProfile;

import java.time.LocalDateTime;
import java.util.UUID;

public record BlockedUserResponse(
        UUID userId,
        String nickname,
        String city,
        String state,
        LocalDateTime blockedAt
) {
    public static BlockedUserResponse from(UserBlock block, UserProfile profile) {
        return new BlockedUserResponse(
                block.getBlockedId(),
                profile != null ? profile.getNickname() : null,
                profile != null && profile.isShowCityStatePublicly() ? profile.getCity() : null,
                profile != null && profile.isShowCityStatePublicly() ? profile.getState() : null,
                block.getCreatedAt()
        );
    }
}
