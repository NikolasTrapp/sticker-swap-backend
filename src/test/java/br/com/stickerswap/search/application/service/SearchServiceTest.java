package br.com.stickerswap.search.application.service;

import br.com.stickerswap.album.domain.model.Album;
import br.com.stickerswap.album.domain.model.Sticker;
import br.com.stickerswap.album.infrastructure.persistence.AlbumRepository;
import br.com.stickerswap.album.infrastructure.persistence.StickerRepository;
import br.com.stickerswap.collection.domain.model.UserRepeatedSticker;
import br.com.stickerswap.collection.infrastructure.persistence.UserRepeatedStickerRepository;
import br.com.stickerswap.collection.infrastructure.persistence.UserWantedStickerRepository;
import br.com.stickerswap.identity.domain.model.User;
import br.com.stickerswap.identity.infrastructure.persistence.UserRepository;
import br.com.stickerswap.profile.domain.model.UserProfile;
import br.com.stickerswap.profile.infrastructure.persistence.UserProfileRepository;
import br.com.stickerswap.search.application.dto.HolderResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SearchServiceTest {

    @Mock UserRepeatedStickerRepository repeatedRepo;
    @Mock UserWantedStickerRepository wantedRepo;
    @Mock UserProfileRepository profileRepo;
    @Mock UserRepository userRepo;
    @Mock AlbumRepository albumRepo;
    @Mock StickerRepository stickerRepo;

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
    void returnsEmptyPage_whenNoHolders() {
        when(repeatedRepo.findByAlbumIdAndStickerIdAndQuantityGreaterThan(ALBUM_ID, STICKER_ID, 0))
                .thenReturn(List.of());

        Page<HolderResponse> result = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), PageRequest.of(0, 20));

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void excludesSearcher_fromResults() {
        when(repeatedRepo.findByAlbumIdAndStickerIdAndQuantityGreaterThan(ALBUM_ID, STICKER_ID, 0))
                .thenReturn(List.of(holder(SEARCHER_ID, 3)));

        Page<HolderResponse> result = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), PageRequest.of(0, 20));

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void excludesBlockedUsers() {
        UUID blockedId = UUID.randomUUID();
        when(repeatedRepo.findByAlbumIdAndStickerIdAndQuantityGreaterThan(ALBUM_ID, STICKER_ID, 0))
                .thenReturn(List.of(holder(blockedId, 2)));

        Page<HolderResponse> result = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(blockedId), PageRequest.of(0, 20));

        assertThat(result.isEmpty()).isTrue();
    }

    @Test
    void sortsSameCityFirst_thenDifferentCity_thenNoCity() {
        UUID sameCityId = UUID.randomUUID();
        UUID diffCityId = UUID.randomUUID();
        UUID noCityId = UUID.randomUUID();

        stubSearcherProfile("São Paulo", "SP");

        // Input order is intentionally scrambled to verify sort
        when(repeatedRepo.findByAlbumIdAndStickerIdAndQuantityGreaterThan(ALBUM_ID, STICKER_ID, 0))
                .thenReturn(List.of(holder(noCityId, 10), holder(diffCityId, 5), holder(sameCityId, 1)));

        when(profileRepo.findByUserIdIn(any())).thenReturn(List.of(
                profile(sameCityId, "SP", "São Paulo", true),
                profile(diffCityId, "RJ", "Rio de Janeiro", true),
                profile(noCityId, null, null, false)));

        when(userRepo.findAllById(any())).thenReturn(List.of(
                user(sameCityId), user(diffCityId), user(noCityId)));

        Page<HolderResponse> result = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), PageRequest.of(0, 20));

        List<UUID> ids = result.getContent().stream().map(HolderResponse::userId).toList();
        assertThat(ids).containsExactly(sameCityId, diffCityId, noCityId);
    }

    @Test
    void sortsPotentialMatchFirst_withinSameCity() {
        UUID matchId = UUID.randomUUID();
        UUID noMatchId = UUID.randomUUID();

        stubSearcherProfile("São Paulo", "SP");
        UUID searcherStickerA = UUID.randomUUID();
        UserRepeatedSticker searcherRepeated = holder(SEARCHER_ID, 2);
        searcherRepeated.setStickerId(searcherStickerA);
        when(repeatedRepo.findByUserIdAndAlbumId(SEARCHER_ID, ALBUM_ID))
                .thenReturn(List.of(searcherRepeated));

        when(repeatedRepo.findByAlbumIdAndStickerIdAndQuantityGreaterThan(ALBUM_ID, STICKER_ID, 0))
                .thenReturn(List.of(holder(matchId, 1), holder(noMatchId, 2)));

        when(profileRepo.findByUserIdIn(any())).thenReturn(List.of(
                profile(matchId, "SP", "São Paulo", true),
                profile(noMatchId, "SP", "São Paulo", true)));

        when(userRepo.findAllById(any())).thenReturn(List.of(user(matchId), user(noMatchId)));
        when(wantedRepo.findHolderIdsWhoWantAnyOf(any(), any())).thenReturn(List.of(matchId));

        Page<HolderResponse> result = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), PageRequest.of(0, 20));

        List<UUID> ids = result.getContent().stream().map(HolderResponse::userId).toList();
        assertThat(ids).containsExactly(matchId, noMatchId);
    }

    @Test
    void sortsHigherQuantityFirst_whenNoOtherDifferences() {
        UUID lowQtyId = UUID.randomUUID();
        UUID highQtyId = UUID.randomUUID();

        stubSearcherProfile("São Paulo", "SP");

        when(repeatedRepo.findByAlbumIdAndStickerIdAndQuantityGreaterThan(ALBUM_ID, STICKER_ID, 0))
                .thenReturn(List.of(holder(lowQtyId, 1), holder(highQtyId, 5)));

        when(profileRepo.findByUserIdIn(any())).thenReturn(List.of(
                profile(lowQtyId, "SP", "São Paulo", true),
                profile(highQtyId, "SP", "São Paulo", true)));

        when(userRepo.findAllById(any())).thenReturn(List.of(user(lowQtyId), user(highQtyId)));

        Page<HolderResponse> result = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), PageRequest.of(0, 20));

        List<UUID> ids = result.getContent().stream().map(HolderResponse::userId).toList();
        assertThat(ids).containsExactly(highQtyId, lowQtyId);
    }

    @Test
    void honoursPagination() {
        UUID h1 = UUID.randomUUID();
        UUID h2 = UUID.randomUUID();
        UUID h3 = UUID.randomUUID();

        when(repeatedRepo.findByAlbumIdAndStickerIdAndQuantityGreaterThan(ALBUM_ID, STICKER_ID, 0))
                .thenReturn(List.of(holder(h1, 1), holder(h2, 1), holder(h3, 1)));

        when(profileRepo.findByUserIdIn(any())).thenReturn(List.of());
        when(userRepo.findAllById(any())).thenReturn(List.of(user(h1), user(h2), user(h3)));
        when(profileRepo.findByUserId(SEARCHER_ID)).thenReturn(Optional.empty());

        Page<HolderResponse> page0 = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), PageRequest.of(0, 2));
        Page<HolderResponse> page1 = searchService.findHolders(
                SEARCHER_ID, ALBUM_ID, STICKER_ID, Set.of(), PageRequest.of(1, 2));

        assertThat(page0.getContent()).hasSize(2);
        assertThat(page0.getTotalElements()).isEqualTo(3);
        assertThat(page1.getContent()).hasSize(1);
    }

    // ── helpers ─────────────────────────────────────────────────────────────

    private void stubSearcherProfile(String city, String state) {
        UserProfile p = profile(SEARCHER_ID, state, city, true);
        when(profileRepo.findByUserId(SEARCHER_ID)).thenReturn(Optional.of(p));
        when(repeatedRepo.findByUserIdAndAlbumId(eq(SEARCHER_ID), eq(ALBUM_ID))).thenReturn(List.of());
    }

    private UserRepeatedSticker holder(UUID userId, int quantity) {
        UserRepeatedSticker s = new UserRepeatedSticker();
        s.setUserId(userId);
        s.setAlbumId(ALBUM_ID);
        s.setStickerId(STICKER_ID);
        s.setQuantity(quantity);
        return s;
    }

    private UserProfile profile(UUID userId, String state, String city, boolean showPublic) {
        UserProfile p = new UserProfile();
        p.setUserId(userId);
        p.setCity(city);
        p.setState(state);
        p.setShowCityStatePublicly(showPublic);
        return p;
    }

    private User user(UUID id) {
        User u = new User();
        u.setId(id);
        u.setLastActivityAt(Instant.now());
        return u;
    }
}
