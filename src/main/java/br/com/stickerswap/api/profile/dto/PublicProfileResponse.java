package br.com.stickerswap.api.profile.dto;

import br.com.stickerswap.domain.profile.model.UserProfile;

import java.util.UUID;

public record PublicProfileResponse(
        UUID userId,
        String nickname,
        String city,   // null when showCityStatePublicly = false
        String state   // null when showCityStatePublicly = false
) {
    public static PublicProfileResponse from(UserProfile p) {
        boolean showLocation = p.isShowCityStatePublicly();
        return new PublicProfileResponse(
                p.getUserId(),
                p.getNickname(),
                showLocation ? p.getCity() : null,
                showLocation ? p.getState() : null
        );
    }
}
