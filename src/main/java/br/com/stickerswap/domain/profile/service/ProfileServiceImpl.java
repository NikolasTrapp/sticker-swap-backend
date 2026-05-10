package br.com.stickerswap.domain.profile.service;

import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.api.profile.dto.CepLookupResponse;
import br.com.stickerswap.api.profile.dto.MyProfileResponse;
import br.com.stickerswap.api.profile.dto.PublicProfileResponse;
import br.com.stickerswap.api.profile.dto.UpdateProfileRequest;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ProfileServiceImpl implements ProfileService {

    private final UserProfileRepository profileRepository;
    private final UserRepository userRepository;
    private final CepGeocodeService cepGeocodeService;

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
        if (req.showCityStatePublicly() != null) profile.setShowCityStatePublicly(req.showCityStatePublicly());
        if (req.useLocationForSearch() != null)  profile.setUseLocationForSearch(req.useLocationForSearch());

        if (req.cep() != null) {
            String normalizedCep = normalizeCep(req.cep());
            boolean cepChanged = !normalizedCep.equals(normalizeCep(profile.getCep()));
            Optional<CepGeocodeService.CepLocation> location = cepGeocodeService.resolve(normalizedCep);
            if (cepChanged && location.isEmpty()) {
                throw new BusinessRuleException("CEP não encontrado.");
            }

            profile.setCep(normalizedCep);
            location.ifPresent(loc -> {
                profile.setApproximateLatitude(loc.latitude());
                profile.setApproximateLongitude(loc.longitude());
                if (req.city() == null) profile.setCity(loc.city());
                if (req.state() == null) profile.setState(loc.state());
            });
            // Explicit values always override whatever the geocoder returned
            if (req.city() != null)  profile.setCity(req.city());
            if (req.state() != null) profile.setState(req.state());
        } else {
            if (req.city() != null)  profile.setCity(req.city());
            if (req.state() != null) profile.setState(req.state());
        }

        return MyProfileResponse.from(profileRepository.save(profile));
    }

    @Transactional(readOnly = true)
    @Override
    public CepLookupResponse lookupCep(String cep) {
        String normalizedCep = normalizeCep(cep);
        return cepGeocodeService.resolve(normalizedCep)
                .map(location -> CepLookupResponse.found(normalizedCep, location))
                .orElseGet(() -> CepLookupResponse.notFound(normalizedCep));
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

    private static String normalizeCep(String cep) {
        return cep == null ? null : cep.replaceAll("\\D", "");
    }
}
