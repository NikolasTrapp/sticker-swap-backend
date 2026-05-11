package br.com.stickerswap.api.moderation.dto;

import br.com.stickerswap.domain.moderation.model.UserBlock;
import br.com.stickerswap.domain.profile.model.UserProfile;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class BlockedUserResponseTest {

    @Test
    @DisplayName("dado perfil com localização pública, quando from(), então expõe nickname e localização")
    void givenProfileWithPublicLocation_whenFrom_thenExposesNicknameAndLocation() {
        // Arrange
        UUID blockedId = UUID.randomUUID();
        LocalDateTime now = LocalDateTime.now();

        UserBlock block = mock(UserBlock.class);
        when(block.getBlockedId()).thenReturn(blockedId);
        when(block.getCreatedAt()).thenReturn(now);

        UserProfile profile = new UserProfile();
        profile.setNickname("fulano");
        profile.setCity("Curitiba");
        profile.setState("PR");
        profile.setShowCityStatePublicly(true);

        // Act
        BlockedUserResponse response = BlockedUserResponse.from(block, profile);

        // Assert
        assertThat(response.userId()).isEqualTo(blockedId);
        assertThat(response.nickname()).isEqualTo("fulano");
        assertThat(response.city()).isEqualTo("Curitiba");
        assertThat(response.state()).isEqualTo("PR");
        assertThat(response.blockedAt()).isEqualTo(now);
    }

    @Test
    @DisplayName("dado perfil com localização privada, quando from(), então oculta cidade e estado")
    void givenProfileWithPrivateLocation_whenFrom_thenHidesCityAndState() {
        // Arrange
        UserBlock block = mock(UserBlock.class);
        when(block.getBlockedId()).thenReturn(UUID.randomUUID());
        when(block.getCreatedAt()).thenReturn(LocalDateTime.now());

        UserProfile profile = new UserProfile();
        profile.setNickname("sicrano");
        profile.setCity("Manaus");
        profile.setState("AM");
        profile.setShowCityStatePublicly(false);

        // Act
        BlockedUserResponse response = BlockedUserResponse.from(block, profile);

        // Assert
        assertThat(response.nickname()).isEqualTo("sicrano");
        assertThat(response.city()).isNull();
        assertThat(response.state()).isNull();
    }

    @Test
    @DisplayName("dado perfil nulo, quando from(), então retorna nickname e localização nulos")
    void givenNullProfile_whenFrom_thenReturnsNullFields() {
        // Arrange
        UUID blockedId = UUID.randomUUID();
        UserBlock block = mock(UserBlock.class);
        when(block.getBlockedId()).thenReturn(blockedId);
        when(block.getCreatedAt()).thenReturn(LocalDateTime.now());

        // Act
        BlockedUserResponse response = BlockedUserResponse.from(block, null);

        // Assert
        assertThat(response.userId()).isEqualTo(blockedId);
        assertThat(response.nickname()).isNull();
        assertThat(response.city()).isNull();
        assertThat(response.state()).isNull();
    }
}
