package br.com.stickerswap.profile.application.dto;

import br.com.stickerswap.profile.domain.model.UserProfile;

import java.math.BigDecimal;
import java.util.UUID;

public record MyProfileResponse(
        UUID userId,
        String nickname,
        String cep,
        String city,
        String state,
        BigDecimal approximateLatitude,
        BigDecimal approximateLongitude,
        boolean showCityStatePublicly,
        boolean useLocationForSearch
) {
    public static MyProfileResponse from(UserProfile p) {
        return new MyProfileResponse(
                p.getUserId(),
                p.getNickname(),
                p.getCep(),
                p.getCity(),
                p.getState(),
                p.getApproximateLatitude(),
                p.getApproximateLongitude(),
                p.isShowCityStatePublicly(),
                p.isUseLocationForSearch()
        );
    }
}
