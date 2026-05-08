package br.com.stickerswap.album.application.service;

import br.com.stickerswap.album.application.dto.AlbumResponse;
import br.com.stickerswap.album.application.dto.CreateAlbumRequest;
import br.com.stickerswap.album.application.dto.CreateStickerRequest;
import br.com.stickerswap.album.application.dto.StickerResponse;
import br.com.stickerswap.album.application.dto.UpdateAlbumRequest;
import br.com.stickerswap.album.application.dto.UpdateStickerRequest;
import br.com.stickerswap.album.domain.model.Album;
import br.com.stickerswap.album.domain.model.Sticker;
import br.com.stickerswap.album.infrastructure.persistence.AlbumRepository;
import br.com.stickerswap.album.infrastructure.persistence.StickerRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumRepository albumRepo;
    private final StickerRepository stickerRepo;

    @Override
    @Transactional(readOnly = true)
    public Page<AlbumResponse> listActiveAlbums(Pageable pageable) {
        return albumRepo.findByActive(true, pageable).map(AlbumResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlbumResponse> listAlbums(Pageable pageable) {
        return albumRepo.findAll(pageable).map(AlbumResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumResponse getActiveAlbum(UUID albumId) {
        return AlbumResponse.from(requireActiveAlbum(albumId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StickerResponse> listActiveStickers(UUID albumId, Pageable pageable) {
        requireActiveAlbum(albumId);
        return stickerRepo.findByAlbumIdAndActive(albumId, true, pageable).map(StickerResponse::from);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StickerResponse> listStickers(UUID albumId, Pageable pageable) {
        requireAlbum(albumId);
        return stickerRepo.findByAlbumId(albumId, pageable).map(StickerResponse::from);
    }

    @Override
    @Transactional
    public AlbumResponse createAlbum(CreateAlbumRequest req) {
        Album album = new Album();
        album.setName(req.name());
        album.setDescription(req.description());
        album.setYear(req.year());
        album.setActive(true);
        return AlbumResponse.from(albumRepo.save(album));
    }

    @Override
    @Transactional
    public AlbumResponse updateAlbum(UUID albumId, UpdateAlbumRequest req) {
        Album album = requireAlbum(albumId);
        if (req.name() != null) {
            album.setName(req.name());
        }
        if (req.description() != null) {
            album.setDescription(req.description());
        }
        if (req.year() != null) {
            album.setYear(req.year());
        }
        return AlbumResponse.from(albumRepo.save(album));
    }

    @Override
    @Transactional
    public AlbumResponse setAlbumActive(UUID albumId, boolean active) {
        Album album = requireAlbum(albumId);
        album.setActive(active);
        return AlbumResponse.from(albumRepo.save(album));
    }

    @Override
    @Transactional
    public StickerResponse createSticker(UUID albumId, CreateStickerRequest req) {
        requireAlbum(albumId);
        if (stickerRepo.existsByAlbumIdAndNumber(albumId, req.number())) {
            throw new BusinessRuleException("Sticker number already exists in this album");
        }

        Sticker sticker = new Sticker();
        sticker.setAlbumId(albumId);
        sticker.setNumber(req.number());
        sticker.setName(req.name());
        sticker.setDescription(req.description());
        sticker.setActive(true);
        return StickerResponse.from(stickerRepo.save(sticker));
    }

    @Override
    @Transactional
    public StickerResponse updateSticker(UUID stickerId, UpdateStickerRequest req) {
        Sticker sticker = requireSticker(stickerId);
        if (req.number() != null && stickerRepo.existsByAlbumIdAndNumberAndIdNot(
                sticker.getAlbumId(), req.number(), stickerId)) {
            throw new BusinessRuleException("Sticker number already exists in this album");
        }
        if (req.number() != null) {
            sticker.setNumber(req.number());
        }
        if (req.name() != null) {
            sticker.setName(req.name());
        }
        if (req.description() != null) {
            sticker.setDescription(req.description());
        }
        return StickerResponse.from(stickerRepo.save(sticker));
    }

    @Override
    @Transactional
    public StickerResponse setStickerActive(UUID stickerId, boolean active) {
        Sticker sticker = requireSticker(stickerId);
        sticker.setActive(active);
        return StickerResponse.from(stickerRepo.save(sticker));
    }

    private Album requireAlbum(UUID albumId) {
        return albumRepo.findById(albumId)
                .orElseThrow(() -> new ResourceNotFoundException("Album", albumId));
    }

    private Album requireActiveAlbum(UUID albumId) {
        return albumRepo.findByIdAndActive(albumId, true)
                .orElseThrow(() -> new ResourceNotFoundException("Album", albumId));
    }

    private Sticker requireSticker(UUID stickerId) {
        return stickerRepo.findById(stickerId)
                .orElseThrow(() -> new ResourceNotFoundException("Sticker", stickerId));
    }
}
