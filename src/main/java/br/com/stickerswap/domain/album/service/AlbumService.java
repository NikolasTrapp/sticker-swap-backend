package br.com.stickerswap.domain.album.service;

import br.com.stickerswap.api.album.dto.*;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AlbumService {
    Page<AlbumResponse> listActiveAlbums(Pageable pageable);

    Page<AlbumResponse> listAlbums(Pageable pageable);

    AlbumResponse getActiveAlbum(UUID albumId);

    Page<StickerResponse> listActiveStickers(UUID albumId, String query, Pageable pageable);

    Page<StickerResponse> listStickers(UUID albumId, Pageable pageable);

    AlbumResponse createAlbum(CreateAlbumRequest req);

    AlbumResponse updateAlbum(UUID albumId, UpdateAlbumRequest req);

    AlbumResponse setAlbumActive(UUID albumId, boolean active);

    StickerResponse createSticker(UUID albumId, CreateStickerRequest req);

    StickerResponse updateSticker(UUID stickerId, UpdateStickerRequest req);

    StickerResponse setStickerActive(UUID stickerId, boolean active);
}
