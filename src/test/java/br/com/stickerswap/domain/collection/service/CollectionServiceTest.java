package br.com.stickerswap.domain.collection.service;

import br.com.stickerswap.api.collection.dto.CollectionFilter;
import br.com.stickerswap.api.collection.dto.CollectionStickerResponse;
import br.com.stickerswap.api.collection.dto.RepeatedStickerResponse;
import br.com.stickerswap.api.collection.dto.SetRepeatedStickerRequest;
import br.com.stickerswap.api.collection.dto.WantedStickerResponse;
import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.collection.model.UserWantedSticker;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserRepeatedStickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserWantedStickerRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CollectionServiceTest {

    @Mock UserRepeatedStickerRepository repeatedRepo;
    @Mock UserWantedStickerRepository wantedRepo;
    @Mock StickerRepository stickerRepo;
    @Mock AlbumRepository albumRepo;

    @InjectMocks CollectionServiceImpl collectionService;

    private final UUID userId = UUID.randomUUID();
    private final UUID albumId = UUID.randomUUID();
    private final UUID stickerId = UUID.randomUUID();

    // ── listCollection ────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado álbum com figurinhas repetidas e desejadas, quando listar coleção, então mescla estado com aviso de conflito")
    void givenActiveAlbumWithRepeatedAndWantedStickers_whenListCollection_thenMergesStateWithConflictWarning() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 25);
        Sticker sticker = sticker();
        givenAlbumIsActive();
        when(stickerRepo.searchActiveCollectionStickers(
                eq(albumId), eq(userId), eq("001"), eq(false), eq(false), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sticker), pageable, 1));
        when(repeatedRepo.findByUserIdAndStickerIdIn(eq(userId), any())).thenReturn(List.of(repeated(2)));
        when(wantedRepo.findByUserIdAndStickerIdIn(eq(userId), any())).thenReturn(List.of(wanted()));

        // Act
        Page<CollectionStickerResponse> result = collectionService.listCollection(
                userId, albumId, " 001 ", CollectionFilter.ALL, pageable);

        // Assert
        assertThat(result.getTotalElements()).isEqualTo(1);
        CollectionStickerResponse item = result.getContent().getFirst();
        assertThat(item.stickerId()).isEqualTo(stickerId);
        assertThat(item.code()).isEqualTo("001");
        assertThat(item.repeatedQuantity()).isEqualTo(2);
        assertThat(item.wanted()).isTrue();
        assertThat(item.warning()).contains("repetida e desejada");
    }

    @Test
    @DisplayName("dado filtro CONFLICT, quando listar coleção, então repassa flag de conflito ao repositório")
    void givenConflictFilter_whenListCollection_thenPassesConflictFlagToRepository() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 25);
        givenAlbumIsActive();
        when(stickerRepo.searchActiveCollectionStickers(
                eq(albumId), eq(userId), eq(null), eq(false), eq(false), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<CollectionStickerResponse> result = collectionService.listCollection(
                userId, albumId, null, CollectionFilter.CONFLICT, pageable);

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("dado página vazia, quando listar coleção, então não consulta repositórios de estado")
    void givenEmptyPage_whenListCollection_thenSkipsStateRepositoryJoins() {
        // Arrange
        PageRequest pageable = PageRequest.of(0, 25);
        givenAlbumIsActive();
        when(stickerRepo.searchActiveCollectionStickers(
                eq(albumId), eq(userId), eq(null), eq(false), eq(false), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        // Act
        Page<CollectionStickerResponse> result = collectionService.listCollection(
                userId, albumId, null, CollectionFilter.ALL, pageable);

        // Assert
        assertThat(result).isEmpty();
        verifyNoInteractions(repeatedRepo, wantedRepo);
    }

    // ── setRepeated ───────────────────────────────────────────────────────────

    @Test
    @DisplayName("dada quantidade negativa, quando registrar repetida, então lança BusinessRuleException antes de acessar repositório")
    void givenNegativeQuantity_whenSetRepeated_thenThrowsBusinessRule() {
        // Arrange
        SetRepeatedStickerRequest req = new SetRepeatedStickerRequest(-1);

        // Act & Assert
        assertThatThrownBy(() -> collectionService.setRepeated(userId, stickerId, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("quantity");
        verifyNoInteractions(repeatedRepo, stickerRepo, albumRepo);
    }

    @Test
    @DisplayName("dado entrada inexistente, quando registrar repetida, então cria nova entrada com a quantidade correta")
    void givenNoExistingEntry_whenSetRepeated_thenCreatesNewEntry() {
        // Arrange
        SetRepeatedStickerRequest req = new SetRepeatedStickerRequest(3);
        givenStickerIsActive();
        givenAlbumIsActive();
        when(repeatedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.empty());
        when(repeatedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(wantedRepo.existsByUserIdAndStickerId(userId, stickerId)).thenReturn(false);

        // Act
        RepeatedStickerResponse response = collectionService.setRepeated(userId, stickerId, req);

        // Assert
        assertThat(response.quantity()).isEqualTo(3);
        assertThat(response.warning()).isNull();
        verify(repeatedRepo).save(argThat(e -> e.getQuantity() == 3 && userId.equals(e.getUserId())));
    }

    @Test
    @DisplayName("dado entrada existente, quando registrar repetida, então atualiza quantidade e detecta conflito")
    void givenExistingEntry_whenSetRepeated_thenUpdatesQuantityAndSignalsConflict() {
        // Arrange
        SetRepeatedStickerRequest req = new SetRepeatedStickerRequest(5);
        givenStickerIsActive();
        givenAlbumIsActive();
        UserRepeatedSticker existing = repeated(2);
        when(repeatedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.of(existing));
        when(repeatedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(wantedRepo.existsByUserIdAndStickerId(userId, stickerId)).thenReturn(true);

        // Act
        RepeatedStickerResponse response = collectionService.setRepeated(userId, stickerId, req);

        // Assert
        assertThat(response.quantity()).isEqualTo(5);
        assertThat(response.warning()).isNotNull();
        verify(repeatedRepo).save(argThat(e -> e.getQuantity() == 5));
    }

    // ── deleteRepeated ────────────────────────────────────────────────────────

    @Test
    @DisplayName("dada entrada inexistente, quando remover repetida, então lança ResourceNotFoundException sem deletar")
    void givenEntryNotFound_whenDeleteRepeated_thenThrowsNotFound() {
        // Arrange
        when(repeatedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> collectionService.deleteRepeated(userId, stickerId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(repeatedRepo, never()).delete(any());
    }

    @Test
    @DisplayName("dada entrada existente, quando remover repetida, então deleta exatamente essa entrada")
    void givenEntryExists_whenDeleteRepeated_thenDeletesExactEntry() {
        // Arrange
        UserRepeatedSticker entry = repeated(1);
        when(repeatedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.of(entry));

        // Act
        collectionService.deleteRepeated(userId, stickerId);

        // Assert
        verify(repeatedRepo).delete(entry);
    }

    // ── listRepeated ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado álbum ativo com repetida também desejada, quando listar repetidas, então retorna aviso de conflito")
    void givenRepeatedStickerAlsoWanted_whenListRepeated_thenReturnsConflictWarning() {
        // Arrange
        givenAlbumIsActive();
        when(repeatedRepo.findAllByUserIdAndAlbumId(userId, albumId)).thenReturn(List.of(repeated(2)));
        when(wantedRepo.findByUserIdAndAlbumId(userId, albumId)).thenReturn(List.of(wanted()));
        givenStickerIsActive();

        // Act
        List<RepeatedStickerResponse> result = collectionService.listRepeated(userId, albumId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().quantity()).isEqualTo(2);
        assertThat(result.getFirst().warning()).contains("repetida e desejada");
    }

    // ── Wanted ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado álbum ativo com desejada também repetida, quando listar desejadas, então retorna aviso de conflito")
    void givenWantedStickerAlsoRepeated_whenListWanted_thenReturnsConflictWarning() {
        // Arrange
        givenAlbumIsActive();
        when(wantedRepo.findByUserIdAndAlbumId(userId, albumId)).thenReturn(List.of(wanted()));
        when(repeatedRepo.findAllByUserIdAndAlbumId(userId, albumId)).thenReturn(List.of(repeated(1)));
        givenStickerIsActive();

        // Act
        List<WantedStickerResponse> result = collectionService.listWanted(userId, albumId);

        // Assert
        assertThat(result).hasSize(1);
        assertThat(result.getFirst().stickerId()).isEqualTo(stickerId);
        assertThat(result.getFirst().warning()).contains("repetida e desejada");
    }

    @Test
    @DisplayName("dada desejada inexistente, quando registrar desejada, então cria nova entrada")
    void givenNoExistingWanted_whenSetWanted_thenCreatesNewEntry() {
        // Arrange
        givenStickerIsActive();
        givenAlbumIsActive();
        when(wantedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.empty());
        when(wantedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repeatedRepo.existsByUserIdAndStickerId(userId, stickerId)).thenReturn(false);

        // Act
        WantedStickerResponse response = collectionService.setWanted(userId, stickerId);

        // Assert
        assertThat(response.stickerId()).isEqualTo(stickerId);
        assertThat(response.warning()).isNull();
        verify(wantedRepo).save(argThat(e -> userId.equals(e.getUserId())
                && albumId.equals(e.getAlbumId())
                && stickerId.equals(e.getStickerId())));
    }

    @Test
    @DisplayName("dada desejada existente e repetida, quando registrar desejada, então mantém entrada e sinaliza conflito")
    void givenExistingWantedAndRepeated_whenSetWanted_thenSignalsConflict() {
        // Arrange
        givenStickerIsActive();
        givenAlbumIsActive();
        UserWantedSticker existing = wanted();
        when(wantedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.of(existing));
        when(wantedRepo.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(repeatedRepo.existsByUserIdAndStickerId(userId, stickerId)).thenReturn(true);

        // Act
        WantedStickerResponse response = collectionService.setWanted(userId, stickerId);

        // Assert
        assertThat(response.warning()).contains("repetida e desejada");
        verify(wantedRepo).save(existing);
    }

    @Test
    @DisplayName("dada desejada inexistente, quando remover desejada, então lança ResourceNotFoundException sem deletar")
    void givenWantedNotFound_whenDeleteWanted_thenThrowsNotFound() {
        // Arrange
        when(wantedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> collectionService.deleteWanted(userId, stickerId))
                .isInstanceOf(ResourceNotFoundException.class);
        verify(wantedRepo, never()).delete(any());
    }

    @Test
    @DisplayName("dada desejada existente, quando remover desejada, então deleta exatamente essa entrada")
    void givenWantedExists_whenDeleteWanted_thenDeletesExactEntry() {
        // Arrange
        UserWantedSticker entry = wanted();
        when(wantedRepo.findByUserIdAndStickerId(userId, stickerId)).thenReturn(Optional.of(entry));

        // Act
        collectionService.deleteWanted(userId, stickerId);

        // Assert
        verify(wantedRepo).delete(entry);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private void givenAlbumIsActive() {
        Album album = new Album();
        album.setId(albumId);
        when(albumRepo.findByIdAndActive(albumId, true)).thenReturn(Optional.of(album));
    }

    private void givenStickerIsActive() {
        when(stickerRepo.findByIdAndActive(stickerId, true)).thenReturn(Optional.of(sticker()));
    }

    private Sticker sticker() {
        Sticker s = new Sticker();
        s.setId(stickerId);
        s.setAlbumId(albumId);
        s.setCode("001");
        s.setName("Mascote");
        return s;
    }

    private UserRepeatedSticker repeated(int quantity) {
        UserRepeatedSticker r = new UserRepeatedSticker();
        r.setUserId(userId);
        r.setAlbumId(albumId);
        r.setStickerId(stickerId);
        r.setQuantity(quantity);
        return r;
    }

    private UserWantedSticker wanted() {
        UserWantedSticker w = new UserWantedSticker();
        w.setUserId(userId);
        w.setAlbumId(albumId);
        w.setStickerId(stickerId);
        return w;
    }
}
