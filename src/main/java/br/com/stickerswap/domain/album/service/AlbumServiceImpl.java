package br.com.stickerswap.domain.album.service;

import br.com.stickerswap.api.album.dto.AlbumResponse;
import br.com.stickerswap.api.album.dto.CreateAlbumRequest;
import br.com.stickerswap.api.album.dto.CreateStickerRequest;
import br.com.stickerswap.api.album.dto.StickerResponse;
import br.com.stickerswap.api.album.dto.UpdateAlbumRequest;
import br.com.stickerswap.api.album.dto.UpdateStickerRequest;
import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AlbumServiceImpl implements AlbumService {

    private final AlbumRepository albumRepo;
    private final StickerRepository stickerRepo;
    private final br.com.stickerswap.api.album.mapper.AlbumMapper mapper;

    @Override
    @Transactional(readOnly = true)
    public Page<AlbumResponse> listActiveAlbums(Pageable pageable) {
        return albumRepo.findByActive(true, pageable).map(mapper::toAlbumResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<AlbumResponse> listAlbums(Pageable pageable) {
        return albumRepo.findAll(pageable).map(mapper::toAlbumResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public AlbumResponse getActiveAlbum(UUID albumId) {
        return mapper.toAlbumResponse(requireActiveAlbum(albumId));
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StickerResponse> listActiveStickers(UUID albumId, String query, Pageable pageable) {
        requireActiveAlbum(albumId);
        String normalizedQuery = StringUtils.hasText(query) ? query.trim() : null;
        return stickerRepo.searchActiveStickers(albumId, normalizedQuery, pageable).map(mapper::toStickerResponse);
    }

    @Override
    @Transactional(readOnly = true)
    public Page<StickerResponse> listStickers(UUID albumId, Pageable pageable) {
        requireAlbum(albumId);
        return stickerRepo.findByAlbumId(albumId, pageable).map(mapper::toStickerResponse);
    }

    @Override
    @Transactional
    public AlbumResponse createAlbum(CreateAlbumRequest req) {
        Album album = mapper.toAlbum(req);
        return mapper.toAlbumResponse(albumRepo.save(album));
    }

    @Override
    @Transactional
    public AlbumResponse updateAlbum(UUID albumId, UpdateAlbumRequest req) {
        Album album = requireAlbum(albumId);
        mapper.updateAlbum(req, album);
        return mapper.toAlbumResponse(albumRepo.save(album));
    }

    @Override
    @Transactional
    public AlbumResponse setAlbumActive(UUID albumId, boolean active) {
        Album album = requireAlbum(albumId);
        album.setActive(active);
        return mapper.toAlbumResponse(albumRepo.save(album));
    }

    @Override
    @Transactional
    public StickerResponse createSticker(UUID albumId, CreateStickerRequest req) {
        requireAlbum(albumId);
        if (stickerRepo.existsByAlbumIdAndCode(albumId, req.code())) {
            throw new BusinessRuleException("Sticker code already exists in this album");
        }

        Sticker sticker = mapper.toSticker(req, albumId);
        return mapper.toStickerResponse(stickerRepo.save(sticker));
    }

    @Override
    @Transactional
    public StickerResponse updateSticker(UUID stickerId, UpdateStickerRequest req) {
        Sticker sticker = requireSticker(stickerId);
        if (req.code() != null && stickerRepo.existsByAlbumIdAndCodeAndIdNot(
                sticker.getAlbumId(), req.code(), stickerId)) {
            throw new BusinessRuleException("Sticker code already exists in this album");
        }
        
        mapper.updateSticker(req, sticker);
        return mapper.toStickerResponse(stickerRepo.save(sticker));
    }

    @Override
    @Transactional
    public StickerResponse setStickerActive(UUID stickerId, boolean active) {
        Sticker sticker = requireSticker(stickerId);
        sticker.setActive(active);
        return mapper.toStickerResponse(stickerRepo.save(sticker));
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
