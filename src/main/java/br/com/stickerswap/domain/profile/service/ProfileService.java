package br.com.stickerswap.domain.profile.service;

import br.com.stickerswap.api.profile.dto.MyProfileResponse;
import br.com.stickerswap.api.profile.dto.PublicProfileResponse;
import br.com.stickerswap.api.profile.dto.UpdateProfileRequest;

import java.util.UUID;

public interface ProfileService {

    MyProfileResponse getMyProfile(UUID userId);

    MyProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest req);

    PublicProfileResponse getPublicProfile(UUID userId);
}
