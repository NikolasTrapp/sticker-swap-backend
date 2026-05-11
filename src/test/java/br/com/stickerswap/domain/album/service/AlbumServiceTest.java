package br.com.stickerswap.domain.album.service;

import br.com.stickerswap.api.album.dto.*;
import br.com.stickerswap.api.album.mapper.AlbumMapper;
import br.com.stickerswap.domain.album.model.Album;
import br.com.stickerswap.domain.album.model.Sticker;
import br.com.stickerswap.infrastructure.repository.album.AlbumRepository;
import br.com.stickerswap.infrastructure.repository.album.StickerRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class AlbumServiceTest {

    @Mock private AlbumRepository albumRepo;
    @Mock private StickerRepository stickerRepo;
    @Mock private AlbumMapper mapper;

    @InjectMocks private AlbumServiceImpl albumService;

    private UUID albumId;
    private UUID stickerId;
    private Album album;
    private Sticker sticker;

    @BeforeEach
    void setUp() {
        albumId = UUID.randomUUID();
        stickerId = UUID.randomUUID();
        album = new Album();
        album.setId(albumId);
        album.setActive(true);
        sticker = new Sticker();
        sticker.setId(stickerId);
        sticker.setAlbumId(albumId);
        sticker.setActive(true);
    }

    @Test
    void listActiveAlbums_ReturnsMappedPage() {
        Pageable pageable = PageRequest.of(0, 10);
        when(albumRepo.findByActive(true, pageable)).thenReturn(new PageImpl<>(List.of(album)));
        when(mapper.toAlbumResponse(album)).thenReturn(mock(AlbumResponse.class));

        Page<AlbumResponse> result = albumService.listActiveAlbums(pageable);

        assertThat(result.getContent()).hasSize(1);
        verify(albumRepo).findByActive(true, pageable);
    }

    @Test
    void getActiveAlbum_WhenFoundAndActive_ReturnsResponse() {
        when(albumRepo.findByIdAndActive(albumId, true)).thenReturn(Optional.of(album));
        when(mapper.toAlbumResponse(album)).thenReturn(mock(AlbumResponse.class));

        AlbumResponse result = albumService.getActiveAlbum(albumId);

        assertThat(result).isNotNull();
    }

    @Test
    void getActiveAlbum_WhenNotFound_ThrowsException() {
        when(albumRepo.findByIdAndActive(albumId, true)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> albumService.getActiveAlbum(albumId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void createAlbum_SavesAndReturnsResponse() {
        CreateAlbumRequest req = new CreateAlbumRequest("Test", "Desc", null);
        when(mapper.toAlbum(req)).thenReturn(album);
        when(albumRepo.save(album)).thenReturn(album);
        when(mapper.toAlbumResponse(album)).thenReturn(mock(AlbumResponse.class));

        AlbumResponse result = albumService.createAlbum(req);

        assertThat(result).isNotNull();
        verify(albumRepo).save(album);
    }

    @Test
    void createSticker_WhenCodeExists_ThrowsException() {
        CreateStickerRequest req = new CreateStickerRequest("S1", "Sticker 1", null);
        when(albumRepo.findById(albumId)).thenReturn(Optional.of(album));
        when(stickerRepo.existsByAlbumIdAndCode(albumId, "S1")).thenReturn(true);

        assertThatThrownBy(() -> albumService.createSticker(albumId, req))
                .isInstanceOf(BusinessRuleException.class)
                .hasMessageContaining("already exists");
    }

    @Test
    void createSticker_SavesAndReturnsResponse() {
        CreateStickerRequest req = new CreateStickerRequest("S1", "Sticker 1", null);
        when(albumRepo.findById(albumId)).thenReturn(Optional.of(album));
        when(stickerRepo.existsByAlbumIdAndCode(albumId, "S1")).thenReturn(false);
        when(mapper.toSticker(req, albumId)).thenReturn(sticker);
        when(stickerRepo.save(sticker)).thenReturn(sticker);
        when(mapper.toStickerResponse(sticker)).thenReturn(mock(StickerResponse.class));

        StickerResponse result = albumService.createSticker(albumId, req);

        assertThat(result).isNotNull();
        verify(stickerRepo).save(sticker);
    }

    @Test
    void updateSticker_WhenCodeExistsInOtherSticker_ThrowsException() {
        UpdateStickerRequest req = new UpdateStickerRequest("S2", "Updated", null);
        when(stickerRepo.findById(stickerId)).thenReturn(Optional.of(sticker));
        when(stickerRepo.existsByAlbumIdAndCodeAndIdNot(albumId, "S2", stickerId)).thenReturn(true);

        assertThatThrownBy(() -> albumService.updateSticker(stickerId, req))
                .isInstanceOf(BusinessRuleException.class);
    }

    @Test
    void setStickerActive_UpdatesAndSaves() {
        when(stickerRepo.findById(stickerId)).thenReturn(Optional.of(sticker));
        when(stickerRepo.save(sticker)).thenReturn(sticker);
        when(mapper.toStickerResponse(sticker)).thenReturn(mock(StickerResponse.class));

        albumService.setStickerActive(stickerId, false);

        assertThat(sticker.isActive()).isFalse();
        verify(stickerRepo).save(sticker);
    }
}
