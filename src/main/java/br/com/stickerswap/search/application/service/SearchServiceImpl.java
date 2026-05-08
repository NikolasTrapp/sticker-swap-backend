package br.com.stickerswap.search.application.service;

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
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final UserRepeatedStickerRepository repeatedRepo;
    private final UserWantedStickerRepository wantedRepo;
    private final UserProfileRepository profileRepo;
    private final UserRepository userRepo;
    private final AlbumRepository albumRepo;
    private final StickerRepository stickerRepo;

    @Transactional(readOnly = true)
    @Override
    public Page<HolderResponse> findHolders(UUID searcherId, UUID albumId, UUID stickerId,
                                            Set<UUID> excludedUserIds, Pageable pageable) {
        albumRepo.findByIdAndActive(albumId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Album", albumId));
        stickerRepo.findByIdAndActive(stickerId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker", stickerId));

        // 1. All users holding this sticker with qty > 0
        List<UserRepeatedSticker> holders = repeatedRepo
                .findByAlbumIdAndStickerIdAndQuantityGreaterThan(albumId, stickerId, 0);

        // 2. Exclude searcher and blocked users
        Set<UUID> excluded = new HashSet<>(excludedUserIds);
        excluded.add(searcherId);
        List<UserRepeatedSticker> eligible = holders.stream()
                .filter(h -> !excluded.contains(h.getUserId()))
                .toList();

        if (eligible.isEmpty()) {
            return Page.empty(pageable);
        }

        Set<UUID> holderIds = eligible.stream().map(UserRepeatedSticker::getUserId)
                .collect(Collectors.toSet());

        // 3. Batch-fetch profiles and users
        Map<UUID, UserProfile> profiles = profileRepo.findByUserIdIn(holderIds).stream()
                .collect(Collectors.toMap(UserProfile::getUserId, p -> p));
        Map<UUID, User> users = userRepo.findAllById(holderIds).stream()
                .collect(Collectors.toMap(User::getId, u -> u));

        // 4. Searcher data for ordering
        UserProfile searcherProfile = profileRepo.findByUserId(searcherId).orElse(null);
        Set<UUID> searcherRepeatedIds = repeatedRepo.findByUserIdAndAlbumId(searcherId, albumId)
                .stream().filter(r -> r.getQuantity() > 0)
                .map(UserRepeatedSticker::getStickerId).collect(Collectors.toSet());

        // 5. Potential matches: holders who want at least one sticker the searcher has repeated
        Set<UUID> potentialMatchIds = searcherRepeatedIds.isEmpty() ? Set.of() :
                new HashSet<>(wantedRepo.findHolderIdsWhoWantAnyOf(holderIds, searcherRepeatedIds));

        // 6. Build and sort holder records
        List<HolderResponse> sorted = eligible.stream()
                .map(h -> buildResponse(h, profiles.get(h.getUserId()),
                        users.get(h.getUserId()), potentialMatchIds.contains(h.getUserId())))
                .sorted(comparator(searcherProfile))
                .toList();

        // 7. Manual pagination
        int total = sorted.size();
        int from = (int) pageable.getOffset();
        int to = Math.min(from + pageable.getPageSize(), total);
        List<HolderResponse> page = from >= total ? List.of() : sorted.subList(from, to);
        return new PageImpl<>(page, pageable, total);
    }

    private HolderResponse buildResponse(UserRepeatedSticker h, UserProfile profile,
                                         User user, boolean isPotentialMatch) {
        String city = null;
        String state = null;
        if (profile != null && profile.isShowCityStatePublicly()) {
            city = profile.getCity();
            state = profile.getState();
        }
        Instant lastActivity = user != null ? user.getLastActivityAt() : null;
        String nickname = profile != null ? profile.getNickname() : null;
        return new HolderResponse(h.getUserId(), nickname, city, state,
                h.getQuantity(), isPotentialMatch, lastActivity);
    }

    private Comparator<HolderResponse> comparator(UserProfile searcher) {
        return Comparator
                // 1. Same city/state as searcher (if searcher has city)
                .<HolderResponse, Boolean>comparing(h -> !sameCity(h, searcher))
                // 2. Users with no city go last
                .thenComparing(h -> !hasPublicCity(h))
                // 3. Potential match first
                .thenComparing(h -> !h.isPotentialMatch())
                // 4. More quantity first
                .thenComparing(Comparator.comparingInt(HolderResponse::quantity).reversed())
                // 5. Most recent activity first
                .thenComparing(h -> h.lastActivityAt() == null ? Instant.MIN : h.lastActivityAt(),
                        Comparator.reverseOrder());
    }

    private boolean sameCity(HolderResponse h, UserProfile searcher) {
        if (searcher == null || searcher.getCity() == null) return false;
        return searcher.getCity().equalsIgnoreCase(h.city()) &&
               Objects.equals(searcher.getState(), h.state());
    }

    private boolean hasPublicCity(HolderResponse h) {
        return h.city() != null;
    }
}
