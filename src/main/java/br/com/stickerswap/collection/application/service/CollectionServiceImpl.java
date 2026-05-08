package br.com.stickerswap.collection.application.service;

import br.com.stickerswap.album.domain.model.Sticker;
import br.com.stickerswap.album.infrastructure.persistence.AlbumRepository;
import br.com.stickerswap.album.infrastructure.persistence.StickerRepository;
import br.com.stickerswap.collection.application.dto.RepeatedStickerResponse;
import br.com.stickerswap.collection.application.dto.SetRepeatedStickerRequest;
import br.com.stickerswap.collection.application.dto.WantedStickerResponse;
import br.com.stickerswap.collection.domain.model.UserRepeatedSticker;
import br.com.stickerswap.collection.domain.model.UserWantedSticker;
import br.com.stickerswap.collection.infrastructure.persistence.UserRepeatedStickerRepository;
import br.com.stickerswap.collection.infrastructure.persistence.UserWantedStickerRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class CollectionServiceImpl implements CollectionService {

    private final UserRepeatedStickerRepository repeatedRepo;
    private final UserWantedStickerRepository wantedRepo;
    private final StickerRepository stickerRepo;
    private final AlbumRepository albumRepo;

    // ── Repeated ─────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<RepeatedStickerResponse> listRepeated(UUID userId, UUID albumId) {
        requireActiveAlbum(albumId);
        List<UserRepeatedSticker> entries = repeatedRepo.findByUserIdAndAlbumId(userId, albumId);
        Set<UUID> wantedIds = wantedRepo.findByUserIdAndAlbumId(userId, albumId).stream()
                .map(UserWantedSticker::getStickerId).collect(Collectors.toSet());
        return entries.stream().map(e -> {
            Sticker s = requireActiveSticker(e.getStickerId());
            return RepeatedStickerResponse.from(e, s, wantedIds.contains(s.getId()));
        }).toList();
    }

    @Transactional
    @Override
    public RepeatedStickerResponse setRepeated(UUID userId, UUID stickerId, SetRepeatedStickerRequest req) {
        if (req.quantity() < 0) {
            throw new BusinessRuleException("quantity must be zero or positive");
        }
        Sticker sticker = requireActiveSticker(stickerId);
        requireActiveAlbum(sticker.getAlbumId());

        UserRepeatedSticker entry = repeatedRepo.findByUserIdAndStickerId(userId, stickerId)
                .orElseGet(() -> {
                    UserRepeatedSticker r = new UserRepeatedSticker();
                    r.setUserId(userId);
                    r.setAlbumId(sticker.getAlbumId());
                    r.setStickerId(stickerId);
                    return r;
                });
        entry.setQuantity(req.quantity());
        repeatedRepo.save(entry);

        boolean alsoWanted = wantedRepo.existsByUserIdAndStickerId(userId, stickerId);
        return RepeatedStickerResponse.from(entry, sticker, alsoWanted);
    }

    @Transactional
    @Override
    public void deleteRepeated(UUID userId, UUID stickerId) {
        UserRepeatedSticker entry = repeatedRepo.findByUserIdAndStickerId(userId, stickerId)
                .orElseThrow(() -> new ResourceNotFoundException("RepeatedSticker", stickerId));
        repeatedRepo.delete(entry);
    }

    // ── Wanted ───────────────────────────────────────────────────────────────

    @Transactional(readOnly = true)
    @Override
    public List<WantedStickerResponse> listWanted(UUID userId, UUID albumId) {
        requireActiveAlbum(albumId);
        List<UserWantedSticker> entries = wantedRepo.findByUserIdAndAlbumId(userId, albumId);
        Set<UUID> repeatedIds = repeatedRepo.findByUserIdAndAlbumId(userId, albumId).stream()
                .map(UserRepeatedSticker::getStickerId).collect(Collectors.toSet());
        return entries.stream().map(e -> {
            Sticker s = requireActiveSticker(e.getStickerId());
            return WantedStickerResponse.from(e, s, repeatedIds.contains(s.getId()));
        }).toList();
    }

    @Transactional
    @Override
    public WantedStickerResponse setWanted(UUID userId, UUID stickerId) {
        Sticker sticker = requireActiveSticker(stickerId);
        requireActiveAlbum(sticker.getAlbumId());

        UserWantedSticker entry = wantedRepo.findByUserIdAndStickerId(userId, stickerId)
                .orElseGet(() -> {
                    UserWantedSticker w = new UserWantedSticker();
                    w.setUserId(userId);
                    w.setAlbumId(sticker.getAlbumId());
                    w.setStickerId(stickerId);
                    return w;
                });
        wantedRepo.save(entry);

        boolean alsoRepeated = repeatedRepo.existsByUserIdAndStickerId(userId, stickerId);
        return WantedStickerResponse.from(entry, sticker, alsoRepeated);
    }

    @Transactional
    @Override
    public void deleteWanted(UUID userId, UUID stickerId) {
        UserWantedSticker entry = wantedRepo.findByUserIdAndStickerId(userId, stickerId)
                .orElseThrow(() -> new ResourceNotFoundException("WantedSticker", stickerId));
        wantedRepo.delete(entry);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Sticker requireActiveSticker(UUID stickerId) {
        return stickerRepo.findByIdAndActive(stickerId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker", stickerId));
    }

    private void requireActiveAlbum(UUID albumId) {
        albumRepo.findByIdAndActive(albumId, true)
                .orElseThrow(() -> new BusinessRuleException("Album is not active: " + albumId));
    }
}
