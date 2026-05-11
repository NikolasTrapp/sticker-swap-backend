package br.com.stickerswap.domain.profile.service;

import br.com.stickerswap.api.profile.dto.UpdateProfileRequest;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
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

    @Test
    @DisplayName("dado CEP encontrado, quando lookupCep(), então retorna resposta com cidade e estado")
    void givenFoundCep_whenLookupCep_thenReturnsFoundResponse() {
        // Arrange
        var location = new CepGeocodeService.CepLocation("Porto Alegre", "RS", null, null);
        when(cepGeocodeService.resolve("90040060")).thenReturn(Optional.of(location));

        // Act
        var response = profileService.lookupCep("90040-060");

        // Assert
        assertThat(response.found()).isTrue();
        assertThat(response.city()).isEqualTo("Porto Alegre");
        assertThat(response.state()).isEqualTo("RS");
    }

    @Test
    @DisplayName("dado mesmo CEP, quando updateMyProfile(), então geocoding não lança erro mesmo sem resultado")
    void givenSameCep_whenUpdateMyProfile_thenDoesNotThrowEvenIfGeocodeEmpty() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.forUser(userId);
        profile.setCep("01310100");
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(cepGeocodeService.resolve("01310100")).thenReturn(Optional.empty());
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act — same CEP; cepChanged=false so empty geocode result is allowed
        var response = profileService.updateMyProfile(userId,
                new UpdateProfileRequest(null, "01310-100", null, null, null, null));

        // Assert
        assertThat(response.cep()).isEqualTo("01310100");
        verify(profileRepository).save(any());
    }

    @Test
    @DisplayName("dado nickname e preferências no request, quando updateMyProfile(), então atualiza todos os campos")
    void givenNicknameAndPreferences_whenUpdateMyProfile_thenUpdatesAllFields() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.forUser(userId);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        var response = profileService.updateMyProfile(userId,
                new UpdateProfileRequest("novo-nick", null, null, null, true, false));

        // Assert
        assertThat(response.nickname()).isEqualTo("novo-nick");
        assertThat(response.showCityStatePublicly()).isTrue();
        assertThat(response.useLocationForSearch()).isFalse();
    }

    @Test
    @DisplayName("dado usuário inexistente, quando getPublicProfile(), então lança ResourceNotFoundException")
    void givenNonExistentUser_whenGetPublicProfile_thenThrowsResourceNotFoundException() {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(userRepository.existsById(userId)).thenReturn(false);

        // Act / Assert
        assertThatThrownBy(() -> profileService.getPublicProfile(userId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    @DisplayName("dado usuário existente com perfil, quando getPublicProfile(), então retorna perfil público")
    void givenExistingUserWithProfile_whenGetPublicProfile_thenReturnsPublicProfile() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.forUser(userId);
        profile.setNickname("joao");
        profile.setShowCityStatePublicly(true);
        profile.setCity("Salvador");
        profile.setState("BA");
        when(userRepository.existsById(userId)).thenReturn(true);
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));

        // Act
        var response = profileService.getPublicProfile(userId);

        // Assert
        assertThat(response.nickname()).isEqualTo("joao");
        assertThat(response.city()).isEqualTo("Salvador");
    }

    @Test
    @DisplayName("dado CEP com valor explícito de cidade no request, quando updateMyProfile(), então cidade do request tem prioridade")
    void givenCepWithExplicitCity_whenUpdateMyProfile_thenRequestCityOverridesGeocode() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = UserProfile.forUser(userId);
        var location = new CepGeocodeService.CepLocation("São Paulo", "SP",
                new BigDecimal("-23.5"), new BigDecimal("-46.6"));
        when(profileRepository.findByUserId(userId)).thenReturn(Optional.of(profile));
        when(cepGeocodeService.resolve("01310100")).thenReturn(Optional.of(location));
        when(profileRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act — explicit city overrides geocoded city
        var response = profileService.updateMyProfile(userId,
                new UpdateProfileRequest(null, "01310-100", "Centro", "SP", null, null));

        // Assert
        assertThat(response.city()).isEqualTo("Centro");
        assertThat(response.state()).isEqualTo("SP");
    }
}
