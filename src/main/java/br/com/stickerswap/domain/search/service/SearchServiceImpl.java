package br.com.stickerswap.domain.search.service;

import br.com.stickerswap.api.search.dto.HolderResponse;
import br.com.stickerswap.domain.collection.model.UserRepeatedSticker;
import br.com.stickerswap.domain.profile.model.UserProfile;
import br.com.stickerswap.domain.search.model.HolderSearchCriteria;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.infrastructure.repository.collection.UserRepeatedStickerRepository;
import br.com.stickerswap.infrastructure.repository.profile.UserProfileRepository;
import br.com.stickerswap.infrastructure.repository.search.HolderSearchRepository;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.HashSet;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class SearchServiceImpl implements SearchService {

    private final HolderSearchRepository holderSearchRepo;
    private final AlbumRepository albumRepo;
    private final StickerRepository stickerRepo;
    private final UserProfileRepository profileRepo;
    private final UserRepeatedStickerRepository repeatedRepo;

    @Transactional(readOnly = true)
    @Override
    public Page<HolderResponse> findHolders(UUID searcherId, UUID albumId, UUID stickerId,
                                            Set<UUID> excludedUserIds, Pageable pageable) {
        albumRepo.findByIdAndActive(albumId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Album", albumId));
        stickerRepo.findByIdAndActive(stickerId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker", stickerId));

        UserProfile searcherProfile = profileRepo.findByUserId(searcherId).orElse(null);

        Set<UUID> searcherRepeatedStickerIds = repeatedRepo
                .findAllByUserIdAndAlbumId(searcherId, albumId).stream()
                .filter(r -> r.getQuantity() > 0)
                .map(UserRepeatedSticker::getStickerId)
                .collect(Collectors.toSet());

        BigDecimal lat = null;
        BigDecimal lon = null;
        if (searcherProfile != null && searcherProfile.isUseLocationForSearch()) {
            lat = searcherProfile.getApproximateLatitude();
            lon = searcherProfile.getApproximateLongitude();
        }

        Set<UUID> excluded = new HashSet<>(excludedUserIds);
        excluded.add(searcherId);

        HolderSearchCriteria criteria = new HolderSearchCriteria(
                albumId, stickerId, excluded, searcherRepeatedStickerIds, lat, lon);

        return holderSearchRepo.findHolders(criteria, pageable);
    }
}
