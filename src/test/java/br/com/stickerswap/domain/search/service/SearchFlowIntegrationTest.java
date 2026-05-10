package br.com.stickerswap.domain.search.service;

import br.com.stickerswap.api.search.dto.HolderResponse;
import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.collection.model.UserWantedSticker;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserRepeatedStickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserWantedStickerRepository;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SearchFlowIntegrationTest extends PostgresIntegrationTest {

    // Approximate coordinates used in tests
    // Curitiba:  lat=-25.4284, lon=-49.2733
    // São Paulo: lat=-23.5505, lon=-46.6333

    @Autowired SearchService searchService;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository profileRepository;
    @Autowired AlbumRepository albumRepository;
    @Autowired StickerRepository stickerRepository;
    @Autowired UserRepeatedStickerRepository repeatedStickerRepository;
    @Autowired UserWantedStickerRepository wantedStickerRepository;

    /**
     * Verifies the full ordering cascade pushed to the database:
     *  1. Holders with location come before holders without
     *  2. Within same proximity, potential matches come first
     *  3. Among tied users, higher quantity first
     *  4. Excluded users (blocked + searcher) are absent
     */
    @Test
    void proximityAndMatchOrderingIsComputedAtDbLevel() {
        Album album = album();
        Sticker target = sticker(album, "001", "Target");
        Sticker offer = sticker(album, "002", "Offer");

        // Searcher in Curitiba with one repeated sticker that others may want
        User searcher = user("searcher");
        profileWithLocation(searcher, "Searcher", -25.4284, -49.2733, true);
        repeated(searcher, album, target, 1);
        repeated(searcher, album, offer, 2);

        // Same city — potential match (wants the searcher's offer), quantity=1
        User curPotential = user("cur-potential");
        profileWithLocation(curPotential, "CurPotential", -25.4284, -49.2733, true);
        repeated(curPotential, album, target, 1);
        wanted(curPotential, album, offer);

        // Same city — no match, quantity=5 (more than curPotential but no match)
        User curHighQty = user("cur-high-qty");
        profileWithLocation(curHighQty, "CurHighQty", -25.4284, -49.2733, true);
        repeated(curHighQty, album, target, 5);

        // Different city (São Paulo) — no match, quantity=10
        User spHolder = user("sp-holder");
        profileWithLocation(spHolder, "SpHolder", -23.5505, -46.6333, true);
        repeated(spHolder, album, target, 10);

        // No location set — must appear last regardless of quantity
        User noLocHolder = user("no-loc");
        profileNoLocation(noLocHolder, "NoLoc");
        repeated(noLocHolder, album, target, 99);

        // Blocked — must not appear
        User blocked = user("blocked");
        profileWithLocation(blocked, "Blocked", -25.4284, -49.2733, true);
        repeated(blocked, album, target, 99);

        Page<HolderResponse> page = searchService.findHolders(
                searcher.getId(), album.getId(), target.getId(),
                Set.of(blocked.getId()),
                PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(4);
        assertThat(page.getContent()).extracting(HolderResponse::userId)
                .containsExactly(
                        curPotential.getId(),   // same city, potential match
                        curHighQty.getId(),     // same city, no match, qty=5
                        spHolder.getId(),       // far city, no match, qty=10
                        noLocHolder.getId());   // no location → always last

        assertThat(page.getContent().getFirst().isPotentialMatch()).isTrue();
        assertThat(page.getContent().get(1).isPotentialMatch()).isFalse();
        assertThat(page.getContent()).extracting(HolderResponse::userId)
                .doesNotContain(searcher.getId(), blocked.getId());
    }

    /**
     * Verifies that pagination is delegated to the database:
     * pages are non-overlapping and totals are correct.
     */
    @Test
    void paginationIsHandledByDatabase() {
        Album album = album();
        Sticker target = sticker(album, "001", "Target");

        User searcher = user("pag-searcher");
        profileNoLocation(searcher, "Searcher");

        // Create 5 holders with distinct quantities (1..5) so ordering is stable
        for (int i = 1; i <= 5; i++) {
            User holder = user("pag-holder-" + i);
            profileNoLocation(holder, "Holder" + i);
            repeated(holder, album, target, i);
        }

        Page<HolderResponse> page0 = searchService.findHolders(
                searcher.getId(), album.getId(), target.getId(), Set.of(), PageRequest.of(0, 3));
        Page<HolderResponse> page1 = searchService.findHolders(
                searcher.getId(), album.getId(), target.getId(), Set.of(), PageRequest.of(1, 3));

        assertThat(page0.getTotalElements()).isEqualTo(5);
        assertThat(page0.getTotalPages()).isEqualTo(2);
        assertThat(page0.getContent()).hasSize(3);
        assertThat(page1.getContent()).hasSize(2);

        var ids0 = page0.getContent().stream().map(HolderResponse::userId).toList();
        var ids1 = page1.getContent().stream().map(HolderResponse::userId).toList();
        assertThat(ids0).doesNotContainAnyElementsOf(ids1);
    }

    /**
     * Verifies that users with use_location_for_search=false are ranked as
     * "no location" (at the end), even if lat/lon columns are populated.
     */
    @Test
    void usersWhoOptedOutOfLocationGoLast() {
        Album album = album();
        Sticker target = sticker(album, "001", "Target");

        User searcher = user("loc-opt-searcher");
        profileWithLocation(searcher, "Searcher", -25.4284, -49.2733, true);

        User optedOut = user("loc-opted-out");
        // Has coordinates but opted out of location search — should sort last
        profileWithLocation(optedOut, "OptedOut", -25.4284, -49.2733, false);
        repeated(optedOut, album, target, 99);

        User normalHolder = user("loc-normal");
        profileWithLocation(normalHolder, "Normal", -23.5505, -46.6333, true);
        repeated(normalHolder, album, target, 1);

        Page<HolderResponse> page = searchService.findHolders(
                searcher.getId(), album.getId(), target.getId(), Set.of(), PageRequest.of(0, 10));

        assertThat(page.getTotalElements()).isEqualTo(2);
        // normalHolder has location (even though farther); optedOut ranks as no-location
        assertThat(page.getContent()).extracting(HolderResponse::userId)
                .containsExactly(normalHolder.getId(), optedOut.getId());
    }

    /**
     * Verifies that city/state are only returned when show_city_state_publicly=true.
     */
    @Test
    void cityStateIsHiddenWhenNotPublic() {
        Album album = album();
        Sticker target = sticker(album, "001", "Target");

        User searcher = user("city-searcher");
        profileNoLocation(searcher, "Searcher");

        User publicHolder = user("city-public");
        UserProfile pub = UserProfile.forUser(publicHolder.getId());
        pub.setNickname("Public");
        pub.setCity("Curitiba");
        pub.setState("PR");
        pub.setShowCityStatePublicly(true);
        profileRepository.save(pub);
        repeated(publicHolder, album, target, 1);

        User privateHolder = user("city-private");
        UserProfile priv = UserProfile.forUser(privateHolder.getId());
        priv.setNickname("Private");
        priv.setCity("Curitiba");
        priv.setState("PR");
        priv.setShowCityStatePublicly(false);
        profileRepository.save(priv);
        repeated(privateHolder, album, target, 1);

        Page<HolderResponse> page = searchService.findHolders(
                searcher.getId(), album.getId(), target.getId(), Set.of(), PageRequest.of(0, 10));

        HolderResponse pub_ = page.getContent().stream()
                .filter(h -> h.userId().equals(publicHolder.getId())).findFirst().orElseThrow();
        HolderResponse priv_ = page.getContent().stream()
                .filter(h -> h.userId().equals(privateHolder.getId())).findFirst().orElseThrow();

        assertThat(pub_.city()).isEqualTo("Curitiba");
        assertThat(pub_.state()).isEqualTo("PR");
        assertThat(priv_.city()).isNull();
        assertThat(priv_.state()).isNull();
    }

    // ── helpers ──────────────────────────────────────────────────────────────

    private Album album() {
        Album album = new Album();
        album.setName("Test Album " + UUID.randomUUID());
        album.setYear(2026);
        return albumRepository.save(album);
    }

    private Sticker sticker(Album album, String code, String name) {
        Sticker s = new Sticker();
        s.setAlbumId(album.getId());
        s.setCode(code);
        s.setName(name);
        return stickerRepository.save(s);
    }

    private User user(String tag) {
        User u = new User();
        u.setEmail(tag + "-" + UUID.randomUUID() + "@test.com");
        u.setPasswordHash("{noop}secret");
        u.setRole(UserRole.USER);
        u.setStatus(UserStatus.ACTIVE);
        u.setEmailVerified(true);
        u.setEmailVerifiedAt(LocalDateTime.now());
        u.setLastActivityAt(LocalDateTime.now());
        return userRepository.save(u);
    }

    private void profileWithLocation(User user, String nickname, double lat, double lon, boolean useLocation) {
        UserProfile p = UserProfile.forUser(user.getId());
        p.setNickname(nickname);
        p.setApproximateLatitude(BigDecimal.valueOf(lat));
        p.setApproximateLongitude(BigDecimal.valueOf(lon));
        p.setUseLocationForSearch(useLocation);
        p.setShowCityStatePublicly(false);
        profileRepository.save(p);
    }

    private void profileNoLocation(User user, String nickname) {
        UserProfile p = UserProfile.forUser(user.getId());
        p.setNickname(nickname);
        p.setUseLocationForSearch(true);
        p.setShowCityStatePublicly(false);
        profileRepository.save(p);
    }

    private void repeated(User user, Album album, Sticker sticker, int quantity) {
        UserRepeatedSticker r = new UserRepeatedSticker();
        r.setUserId(user.getId());
        r.setAlbumId(album.getId());
        r.setStickerId(sticker.getId());
        r.setQuantity(quantity);
        repeatedStickerRepository.save(r);
    }

    private void wanted(User user, Album album, Sticker sticker) {
        UserWantedSticker w = new UserWantedSticker();
        w.setUserId(user.getId());
        w.setAlbumId(album.getId());
        w.setStickerId(sticker.getId());
        wantedStickerRepository.save(w);
    }
}
