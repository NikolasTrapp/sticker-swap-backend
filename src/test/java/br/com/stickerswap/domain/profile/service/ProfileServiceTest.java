package br.com.stickerswap.domain.profile.service;

import br.com.stickerswap.api.profile.dto.UpdateProfileRequest;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProfileServiceTest {

    @Mock UserProfileRepository profileRepository;
    @Mock UserRepository userRepository;
    @Mock CepGeocodeService cepGeocodeService;

    @InjectMocks ProfileServiceImpl profileService;

    @Test
    void updateMyProfile_fillsCityAndStateFromCep() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.forUser(userId);
        var location = new CepGeocodeService.CepLocation(
                "Blumenau",
                "SC",
                new BigDecimal("-26.9187527"),
                new BigDecimal("-49.0660250"));

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(cepGeocodeService.resolve("89037504")).thenReturn(Optional.of(location));
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = profileService.updateMyProfile(userId,
                new UpdateProfileRequest(null, "89037-504", null, null, null, null));

        assertThat(response.cep()).isEqualTo("89037504");
        assertThat(response.city()).isEqualTo("Blumenau");
        assertThat(response.state()).isEqualTo("SC");
        assertThat(response.approximateLatitude()).isEqualByComparingTo("-26.9187527");
        assertThat(response.approximateLongitude()).isEqualByComparingTo("-49.0660250");
    }

    @Test
    void updateMyProfile_rejectsChangedCepWhenNotFound() {
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.forUser(userId);

        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(cepGeocodeService.resolve("99999999")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> profileService.updateMyProfile(userId,
                new UpdateProfileRequest(null, "99999-999", null, null, null, null)))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessage("CEP não encontrado.");

        verify(profileRepository, never()).save(any());
    }

    @Test
    void lookupCep_returnsNotFoundResponseWhenCepDoesNotExist() {
        when(cepGeocodeService.resolve("99999999")).thenReturn(Optional.empty());

        var response = profileService.lookupCep("99999-999");

        assertThat(response.cep()).isEqualTo("99999999");
        assertThat(response.found()).isFalse();
        assertThat(response.city()).isNull();
        assertThat(response.state()).isNull();
    }
}
