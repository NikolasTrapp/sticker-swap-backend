package br.com.stickerswap.domain.collection.service;

import br.com.stickerswap.api.collection.dto.CollectionFilter;
import br.com.stickerswap.api.collection.dto.CollectionStickerResponse;
import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.collection.model.UserWantedSticker;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserRepeatedStickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserWantedStickerRepository;
import org.junit.jupiter.api.BeforeEach;
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
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

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

    @BeforeEach
    void setup() {
        Album album = new Album();
        album.setId(albumId);
        when(albumRepo.findByIdAndActive(albumId, true)).thenReturn(Optional.of(album));
    }

    @Test
    void listCollectionReturnsMergedRepeatedWantedState() {
        PageRequest pageable = PageRequest.of(0, 25);
        Sticker sticker = sticker();
        UserRepeatedSticker repeated = repeated(2);
        UserWantedSticker wanted = wanted();

        when(stickerRepo.searchActiveCollectionStickers(
                eq(albumId), eq(userId), eq("001"), eq(false), eq(false), eq(false), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(sticker), pageable, 1));
        when(repeatedRepo.findByUserIdAndStickerIdIn(eq(userId), any())).thenReturn(List.of(repeated));
        when(wantedRepo.findByUserIdAndStickerIdIn(eq(userId), any())).thenReturn(List.of(wanted));

        Page<CollectionStickerResponse> result = collectionService.listCollection(
                userId, albumId, " 001 ", CollectionFilter.ALL, pageable);

        assertThat(result.getTotalElements()).isEqualTo(1);
        CollectionStickerResponse item = result.getContent().getFirst();
        assertThat(item.stickerId()).isEqualTo(stickerId);
        assertThat(item.code()).isEqualTo("001");
        assertThat(item.repeatedQuantity()).isEqualTo(2);
        assertThat(item.wanted()).isTrue();
        assertThat(item.warning()).contains("repetida e desejada");
    }

    @Test
    void listCollectionPassesConflictFilterToRepository() {
        PageRequest pageable = PageRequest.of(0, 25);
        when(stickerRepo.searchActiveCollectionStickers(
                eq(albumId), eq(userId), eq(null), eq(false), eq(false), eq(true), eq(pageable)))
                .thenReturn(new PageImpl<>(List.of(), pageable, 0));

        Page<CollectionStickerResponse> result = collectionService.listCollection(
                userId, albumId, null, CollectionFilter.CONFLICT, pageable);

        assertThat(result).isEmpty();
    }

    private Sticker sticker() {
        Sticker sticker = new Sticker();
        sticker.setId(stickerId);
        sticker.setAlbumId(albumId);
        sticker.setCode("001");
        sticker.setName("Mascote");
        return sticker;
    }

    private UserRepeatedSticker repeated(int quantity) {
        UserRepeatedSticker repeated = new UserRepeatedSticker();
        repeated.setUserId(userId);
        repeated.setAlbumId(albumId);
        repeated.setStickerId(stickerId);
        repeated.setQuantity(quantity);
        return repeated;
    }

    private UserWantedSticker wanted() {
        UserWantedSticker wanted = new UserWantedSticker();
        wanted.setUserId(userId);
        wanted.setAlbumId(albumId);
        wanted.setStickerId(stickerId);
        return wanted;
    }
}
