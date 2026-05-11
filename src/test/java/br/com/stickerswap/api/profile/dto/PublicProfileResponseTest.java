package br.com.stickerswap.api.profile.dto;

import br.com.stickerswap.domain.profile.model.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PublicProfileResponseTest {

    @Test
    @DisplayName("dado perfil com localização pública, quando from(), então expõe cidade e estado")
    void givenProfileWithPublicLocation_whenFrom_thenExposesCityAndState() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickname("fulano");
        profile.setCity("São Paulo");
        profile.setState("SP");
        profile.setShowCityStatePublicly(true);

        // Act
        PublicProfileResponse response = PublicProfileResponse.from(profile);

        // Assert
        assertThat(response.userId()).isEqualTo(userId);
        assertThat(response.nickname()).isEqualTo("fulano");
        assertThat(response.city()).isEqualTo("São Paulo");
        assertThat(response.state()).isEqualTo("SP");
    }

    @Test
    @DisplayName("dado perfil com localização privada, quando from(), então oculta cidade e estado")
    void givenProfileWithPrivateLocation_whenFrom_thenHidesCityAndState() {
        // Arrange
        UUID userId = UUID.randomUUID();
        UserProfile profile = new UserProfile();
        profile.setUserId(userId);
        profile.setNickname("sicrano");
        profile.setCity("Recife");
        profile.setState("PE");
        profile.setShowCityStatePublicly(false);

        // Act
        PublicProfileResponse response = PublicProfileResponse.from(profile);

        // Assert
        assertThat(response.city()).isNull();
        assertThat(response.state()).isNull();
    }
}
