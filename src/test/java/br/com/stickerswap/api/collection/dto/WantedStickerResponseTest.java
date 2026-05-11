package br.com.stickerswap.api.collection.dto;

import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserWantedSticker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class WantedStickerResponseTest {

    @Test
    @DisplayName("dado figurinha também repetida, quando from(), então adiciona aviso de sobreposição")
    void givenStickerAlsoRepeated_whenFrom_thenAddsOverlapWarning() {
        // Arrange
        UserWantedSticker entry = mock(UserWantedSticker.class);
        when(entry.getId()).thenReturn(UUID.randomUUID());
        Sticker sticker = sticker();

        // Act
        WantedStickerResponse response = WantedStickerResponse.from(entry, sticker, true);

        // Assert
        assertThat(response.warning()).isNotNull();
    }

    @Test
    @DisplayName("dado figurinha não repetida, quando from(), então sem aviso")
    void givenStickerNotRepeated_whenFrom_thenNoWarning() {
        // Arrange
        UserWantedSticker entry = mock(UserWantedSticker.class);
        when(entry.getId()).thenReturn(UUID.randomUUID());
        Sticker sticker = sticker();

        // Act
        WantedStickerResponse response = WantedStickerResponse.from(entry, sticker, false);

        // Assert
        assertThat(response.warning()).isNull();
        assertThat(response.stickerNumber()).isEqualTo("001");
    }

    private Sticker sticker() {
        Sticker s = new Sticker();
        s.setId(UUID.randomUUID());
        s.setCode("001");
        s.setName("Neymar");
        return s;
    }
}
