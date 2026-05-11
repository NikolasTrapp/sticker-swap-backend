package br.com.stickerswap.api.collection.dto;

import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.collection.model.UserWantedSticker;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class CollectionStickerResponseTest {

    @Test
    @DisplayName("dado figurinha com repetida e desejada ao mesmo tempo, quando from(), então adiciona aviso de sobreposição")
    void givenStickerWithBothRepeatedAndWanted_whenFrom_thenAddsOverlapWarning() {
        // Arrange
        Sticker sticker = sticker();
        UserRepeatedSticker repeated = mock(UserRepeatedSticker.class);
        when(repeated.getQuantity()).thenReturn(2);
        UserWantedSticker wanted = mock(UserWantedSticker.class);

        // Act
        CollectionStickerResponse response = CollectionStickerResponse.from(sticker, repeated, wanted);

        // Assert
        assertThat(response.repeatedQuantity()).isEqualTo(2);
        assertThat(response.wanted()).isTrue();
        assertThat(response.warning()).isNotNull();
    }

    @Test
    @DisplayName("dado figurinha sem repetida e sem desejada, quando from(), então sem aviso")
    void givenStickerWithNeitherRepeatedNorWanted_whenFrom_thenNoWarning() {
        // Arrange
        Sticker sticker = sticker();

        // Act
        CollectionStickerResponse response = CollectionStickerResponse.from(sticker, null, null);

        // Assert
        assertThat(response.repeatedQuantity()).isZero();
        assertThat(response.wanted()).isFalse();
        assertThat(response.warning()).isNull();
    }

    @Test
    @DisplayName("dado figurinha com repetida mas sem desejada, quando from(), então sem aviso")
    void givenStickerWithRepeatedButNotWanted_whenFrom_thenNoWarning() {
        // Arrange
        Sticker sticker = sticker();
        UserRepeatedSticker repeated = mock(UserRepeatedSticker.class);
        when(repeated.getQuantity()).thenReturn(3);

        // Act
        CollectionStickerResponse response = CollectionStickerResponse.from(sticker, repeated, null);

        // Assert
        assertThat(response.repeatedQuantity()).isEqualTo(3);
        assertThat(response.wanted()).isFalse();
        assertThat(response.warning()).isNull();
    }

    @Test
    @DisplayName("dado figurinha com zero repetidas e desejada, quando from(), então sem aviso (zero não conta como repetida)")
    void givenStickerWithZeroRepeatedAndWanted_whenFrom_thenNoWarning() {
        // Arrange
        Sticker sticker = sticker();
        UserRepeatedSticker repeated = mock(UserRepeatedSticker.class);
        when(repeated.getQuantity()).thenReturn(0);
        UserWantedSticker wanted = mock(UserWantedSticker.class);

        // Act
        CollectionStickerResponse response = CollectionStickerResponse.from(sticker, repeated, wanted);

        // Assert
        assertThat(response.repeatedQuantity()).isZero();
        assertThat(response.warning()).isNull();
    }

    private Sticker sticker() {
        Sticker s = new Sticker();
        s.setId(UUID.randomUUID());
        s.setCode("001");
        s.setName("Neymar");
        return s;
    }
}
