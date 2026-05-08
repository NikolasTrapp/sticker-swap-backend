package br.com.stickerswap.domain.search.service;

import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.collection.model.UserWantedSticker;
import br.com.stickerswap.infrastructure.repository.collection.UserRepeatedStickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserWantedStickerRepository;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.api.search.dto.HolderResponse;
import br.com.stickerswap.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

import java.time.Instant;
import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class SearchFlowIntegrationTest extends PostgresIntegrationTest {

    @Autowired SearchService searchService;
    @Autowired UserRepository userRepository;
    @Autowired UserProfileRepository profileRepository;
    @Autowired AlbumRepository albumRepository;
    @Autowired StickerRepository stickerRepository;
    @Autowired UserRepeatedStickerRepository repeatedStickerRepository;
    @Autowired UserWantedStickerRepository wantedStickerRepository;

    @Test
    void findHoldersUsesPostgresBackedRepositoriesForExclusionsAndOrdering() {
        Album album = album("World Cup Integration " + UUID.randomUUID());
        Sticker target = sticker(album, "001", "Target sticker");
        Sticker searcherOffer = sticker(album, "002", "Searcher repeated sticker");

        User searcher = user("searcher-" + UUID.randomUUID() + "@example.com", Instant.parse("2026-05-01T10:00:00Z"));
        profile(searcher, "Searcher", "Curitiba", "PR", true);
        repeated(searcher, album, target, 1);
        repeated(searcher, album, searcherOffer, 2);

        User sameCityPotential = user("potential-" + UUID.randomUUID() + "@example.com", Instant.parse("2026-05-04T10:00:00Z"));
        profile(sameCityPotential, "Potential", "Curitiba", "PR", true);
        repeated(sameCityPotential, album, target, 1);
        wanted(sameCityPotential, album, searcherOffer);

        User sameCityHigherQuantity = user("quantity-" + UUID.randomUUID() + "@example.com", Instant.parse("2026-05-05T10:00:00Z"));
        profile(sameCityHigherQuantity, "Quantity", "Curitiba", "PR", true);
        repeated(sameCityHigherQuantity, album, target, 5);

        User otherCity = user("other-" + UUID.randomUUID() + "@example.com", Instant.parse("2026-05-06T10:00:00Z"));
        profile(otherCity, "Other", "Sao Paulo", "SP", true);
        repeated(otherCity, album, target, 10);

        User blocked = user("blocked-" + UUID.randomUUID() + "@example.com", Instant.parse("2026-05-07T10:00:00Z"));
        profile(blocked, "Blocked", "Curitiba", "PR", true);
        repeated(blocked, album, target, 99);

        Page<HolderResponse> result = searchService.findHolders(
                searcher.getId(),
                album.getId(),
                target.getId(),
                Set.of(blocked.getId()),
                PageRequest.of(0, 10));

        assertThat(result.getTotalElements()).isEqualTo(3);
        assertThat(result.getContent().stream().map(HolderResponse::userId))
                .containsExactly(sameCityPotential.getId(), sameCityHigherQuantity.getId(), otherCity.getId());
        assertThat(result.getContent().getFirst().isPotentialMatch()).isTrue();
        assertThat(result.getContent().stream().map(HolderResponse::userId))
                .doesNotContain(searcher.getId(), blocked.getId());
    }

    private Album album(String name) {
        Album album = new Album();
        album.setName(name);
        album.setYear(2026);
        return albumRepository.save(album);
    }

    private Sticker sticker(Album album, String number, String name) {
        Sticker sticker = new Sticker();
        sticker.setAlbumId(album.getId());
        sticker.setNumber(number);
        sticker.setName(name);
        return stickerRepository.save(sticker);
    }

    private User user(String email, Instant lastActivityAt) {
        User user = new User();
        user.setEmail(email);
        user.setPasswordHash("{noop}secret123");
        user.setRole(UserRole.USER);
        user.setStatus(UserStatus.ACTIVE);
        user.setEmailVerified(true);
        user.setEmailVerifiedAt(Instant.now());
        user.setLastActivityAt(lastActivityAt);
        return userRepository.save(user);
    }

    private void profile(User user, String nickname, String city, String state, boolean publicLocation) {
        UserProfile profile = UserProfile.forUser(user.getId());
        profile.setNickname(nickname);
        profile.setCity(city);
        profile.setState(state);
        profile.setShowCityStatePublicly(publicLocation);
        profileRepository.save(profile);
    }

    private void repeated(User user, Album album, Sticker sticker, int quantity) {
        UserRepeatedSticker repeated = new UserRepeatedSticker();
        repeated.setUserId(user.getId());
        repeated.setAlbumId(album.getId());
        repeated.setStickerId(sticker.getId());
        repeated.setQuantity(quantity);
        repeatedStickerRepository.save(repeated);
    }

    private void wanted(User user, Album album, Sticker sticker) {
        UserWantedSticker wanted = new UserWantedSticker();
        wanted.setUserId(user.getId());
        wanted.setAlbumId(album.getId());
        wanted.setStickerId(sticker.getId());
        wantedStickerRepository.save(wanted);
    }
}
