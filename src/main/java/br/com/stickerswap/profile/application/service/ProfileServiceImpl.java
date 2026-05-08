package br.com.stickerswap.profile.application.service;

import br.com.stickerswap.identity.infrastructure.persistence.UserRepository;
import br.com.stickerswap.profile.application.dto.MyProfileResponse;
import br.com.stickerswap.profile.application.dto.PublicProfileResponse;
import br.com.stickerswap.profile.application.dto.UpdateProfileRequest;
import br.com.stickerswap.profile.domain.model.UserProfile;
import br.com.stickerswap.profile.infrastructure.persistence.UserProfileRepository;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;

    @Transactional
    @Override
    public MyProfileResponse getMyProfile(UUID userId) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> profileRepository.save(UserProfile.forUser(userId)));
        return MyProfileResponse.from(profile);
    }

    @Transactional
    @Override
    public MyProfileResponse updateMyProfile(UUID userId, UpdateProfileRequest req) {
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.forUser(userId));

        if (req.nickname() != null)              profile.setNickname(req.nickname());
        if (req.cep() != null)                   profile.setCep(req.cep());
        if (req.city() != null)                  profile.setCity(req.city());
        if (req.state() != null)                 profile.setState(req.state());
        if (req.showCityStatePublicly() != null) profile.setShowCityStatePublicly(req.showCityStatePublicly());
        if (req.useLocationForSearch() != null)  profile.setUseLocationForSearch(req.useLocationForSearch());

        return MyProfileResponse.from(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    @Override
    public PublicProfileResponse getPublicProfile(UUID userId) {
        if (!userRepository.existsById(userId)) {
            throw new ResourceNotFoundException("User", userId);
        }
        UserProfile profile = profileRepository.findByUserId(userId)
                .orElseGet(() -> UserProfile.forUser(userId));
        return PublicProfileResponse.from(profile);
    }
}
