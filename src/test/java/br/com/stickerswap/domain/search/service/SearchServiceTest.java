package br.com.stickerswap.domain.search.service;

import br.com.stickerswap.api.search.dto.HolderResponse;
import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.domain.search.model.HolderSearchCriteria;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserRepeatedStickerRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.infrastructure.repository.search.HolderSearchRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock HolderSearchRepository holderSearchRepo;
    @Mock AlbumRepository albumRepo;
    @Mock StickerRepository stickerRepo;
    @Mock UserProfileRepository profileRepo;
    @Mock UserRepeatedStickerRepository repeatedRepo;

    @InjectMocks SearchServiceImpl searchService;

    static final UUID ALBUM_ID = UUID.randomUUID();
    static final UUID STICKER_ID = UUID.randomUUID();
    static final UUID SEARCHER_ID = UUID.randomUUID();

    @BeforeEach
    void setup() {
        Album album = new Album();
        album.setId(ALBUM_ID);
        when(albumRepo.findByIdAndActive(ALBUM_ID, true)).thenReturn(Optional.of(album));

        Sticker sticker = new Sticker();
        sticker.setId(STICKER_ID);
        when(stickerRepo.findByIdAndActive(STICKER_ID, true)).thenReturn(Optional.of(sticker));
    }

    @Test
    void delegatesSearchWithExcludedUsersAndSearcherRepeatedStickers() {
        Pageable pageable = PageRequest.of(0, 20);
        UUID blockedUserId = UUID.randomUUID();
        UUID searcherRepeatedStickerId = UUID.randomUUID();
        Page<HolderResponse> expected = new PageImpl<>(
                List.of(holderResponse(UUID.randomUUID())),
                pageable,
                1
        );

        when(profileRepo.findByUserId(SEARCHER_ID)).thenReturn(Optional.empty());
        when(repeatedRepo.findAllByUserIdAndAlbumId(SEARCHER_ID, ALBUM_ID)).thenReturn(List.of(
                repeated(searcherRepeatedStickerId, 2),
                repeated(UUID.randomUUID(), 0)
        ));
        when(holderSearchRepo.findHolders(any(), eq(pageable))).thenReturn(expected);

        Page<HolderResponse> result = searchService.findHolders(
                SEARCHER_ID,
                ALBUM_ID,
                STICKER_ID,
                Set.of(blockedUserId),
                pageable
        );

        assertThat(result).isSameAs(expected);

        ArgumentCaptor<HolderSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(HolderSearchCriteria.class);
        verify(holderSearchRepo).findHolders(criteriaCaptor.capture(), eq(pageable));
        HolderSearchCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.albumId()).isEqualTo(ALBUM_ID);
        assertThat(criteria.stickerId()).isEqualTo(STICKER_ID);
        assertThat(criteria.excludedUserIds()).containsExactlyInAnyOrder(SEARCHER_ID, blockedUserId);
        assertThat(criteria.searcherRepeatedStickerIds()).containsExactly(searcherRepeatedStickerId);
        assertThat(criteria.searcherLat()).isNull();
        assertThat(criteria.searcherLon()).isNull();
    }

    @Test
    void includesSearcherCoordinatesWhenLocationSearchIsEnabled() {
        Pageable pageable = PageRequest.of(0, 20);
        BigDecimal lat = new BigDecimal("-25.4284");
        BigDecimal lon = new BigDecimal("-49.2733");
        UserProfile profile = UserProfile.forUser(SEARCHER_ID);
        profile.setApproximateLatitude(lat);
        profile.setApproximateLongitude(lon);
        profile.setUseLocationForSearch(true);

        when(profileRepo.findByUserId(SEARCHER_ID)).thenReturn(Optional.of(profile));
        when(repeatedRepo.findAllByUserIdAndAlbumId(SEARCHER_ID, ALBUM_ID)).thenReturn(List.of());
        when(holderSearchRepo.findHolders(any(), eq(pageable))).thenReturn(new PageImpl<>(List.of(), pageable, 0));

        searchService.findHolders(SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), pageable);

        ArgumentCaptor<HolderSearchCriteria> criteriaCaptor = ArgumentCaptor.forClass(HolderSearchCriteria.class);
        verify(holderSearchRepo).findHolders(criteriaCaptor.capture(), eq(pageable));
        HolderSearchCriteria criteria = criteriaCaptor.getValue();
        assertThat(criteria.searcherLat()).isEqualTo(lat);
        assertThat(criteria.searcherLon()).isEqualTo(lon);
    }

    private UserRepeatedSticker repeated(UUID stickerId, int quantity) {
        UserRepeatedSticker repeated = new UserRepeatedSticker();
        repeated.setUserId(SEARCHER_ID);
        repeated.setAlbumId(ALBUM_ID);
        repeated.setStickerId(stickerId);
        repeated.setQuantity(quantity);
        return repeated;
    }

    private HolderResponse holderResponse(UUID userId) {
        return new HolderResponse(userId, "Holder", "Curitiba", "PR", 1, false, LocalDateTime.now(), null);
    }
}
