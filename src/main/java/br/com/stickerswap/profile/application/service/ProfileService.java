package br.com.stickerswap.profile.application.service;

import br.com.stickerswap.profile.application.dto.MyProfileResponse;
import br.com.stickerswap.profile.application.dto.PublicProfileResponse;
import br.com.stickerswap.profile.application.dto.UpdateProfileRequest;

import java.util.UUID;

public interface ProfileService {

    MyProfileResponse getMyProfile(UUID userId);

    MyProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest req);

    PublicProfileResponse getPublicProfile(UUID userId);
}
