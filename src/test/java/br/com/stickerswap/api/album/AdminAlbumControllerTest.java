package br.com.stickerswap.api.album;

import br.com.stickerswap.api.album.dto.AlbumResponse;
import br.com.stickerswap.api.album.dto.StickerResponse;
import br.com.stickerswap.domain.album.service.AlbumService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.stickerswap.support.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminAlbumControllerTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AlbumService albumService;

    // ── Albums ────────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado admin, quando GET /admin/albums, então retorna 200 com página de álbuns")
    void givenAdmin_whenListAlbums_thenReturns200() throws Exception {
        // Arrange
        when(albumService.listAlbums(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/admin/albums"))
                .andExpect(status().isOk());

        verify(albumService).listAlbums(any(Pageable.class));
    }

    @Test
    @DisplayName("dado payload válido, quando POST /admin/albums, então retorna 201 com álbum criado")
    void givenValidPayload_whenCreateAlbum_thenReturns201() throws Exception {
        // Arrange
        AlbumResponse album = album(UUID.randomUUID());
        when(albumService.createAlbum(any())).thenReturn(album);

        // Act / Assert
        mockMvc.perform(post("/admin/albums")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Copa 2026\",\"year\":2026}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.name").value("Copa 2026"));
    }

    @Test
    @DisplayName("dado álbum existente e payload válido, quando PUT /admin/albums/{id}, então retorna 200 com álbum atualizado")
    void givenExistingAlbumAndValidPayload_whenUpdateAlbum_thenReturns200() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        AlbumResponse updated = album(albumId);
        when(albumService.updateAlbum(eq(albumId), any())).thenReturn(updated);

        // Act / Assert
        mockMvc.perform(put("/admin/albums/{albumId}", albumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"name\":\"Copa 2026\",\"year\":2026}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value(albumId.toString()));
    }

    @Test
    @DisplayName("dado álbum existente, quando PATCH /admin/albums/{id}/activate, então retorna 200")
    void givenExistingAlbum_whenActivateAlbum_thenReturns200() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        when(albumService.setAlbumActive(albumId, true)).thenReturn(album(albumId));

        // Act / Assert
        mockMvc.perform(patch("/admin/albums/{albumId}/activate", albumId))
                .andExpect(status().isOk());

        verify(albumService).setAlbumActive(albumId, true);
    }

    @Test
    @DisplayName("dado álbum existente, quando PATCH /admin/albums/{id}/deactivate, então retorna 200")
    void givenExistingAlbum_whenDeactivateAlbum_thenReturns200() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        when(albumService.setAlbumActive(albumId, false)).thenReturn(album(albumId));

        // Act / Assert
        mockMvc.perform(patch("/admin/albums/{albumId}/deactivate", albumId))
                .andExpect(status().isOk());

        verify(albumService).setAlbumActive(albumId, false);
    }

    // ── Stickers ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado álbum existente, quando GET /admin/albums/{id}/stickers, então retorna 200 com página")
    void givenExistingAlbum_whenListAdminStickers_thenReturns200() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        when(albumService.listStickers(eq(albumId), any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/admin/albums/{albumId}/stickers", albumId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado payload válido, quando POST /admin/albums/{id}/stickers, então retorna 201")
    void givenValidPayload_whenCreateSticker_thenReturns201() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        when(albumService.createSticker(eq(albumId), any())).thenReturn(sticker(albumId));

        // Act / Assert
        mockMvc.perform(post("/admin/albums/{albumId}/stickers", albumId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"001\",\"name\":\"Neymar\"}"))
                .andExpect(status().isCreated());
    }

    @Test
    @DisplayName("dado figurinha existente e payload válido, quando PUT /admin/stickers/{id}, então retorna 200")
    void givenExistingStickerAndPayload_whenUpdateSticker_thenReturns200() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        when(albumService.updateSticker(eq(stickerId), any())).thenReturn(sticker(albumId));

        // Act / Assert
        mockMvc.perform(put("/admin/stickers/{stickerId}", stickerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"code\":\"001\",\"name\":\"Neymar\"}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado figurinha existente, quando PATCH /admin/stickers/{id}/activate, então retorna 200")
    void givenExistingSticker_whenActivateSticker_thenReturns200() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        when(albumService.setStickerActive(stickerId, true)).thenReturn(sticker(albumId));

        // Act / Assert
        mockMvc.perform(patch("/admin/stickers/{stickerId}/activate", stickerId))
                .andExpect(status().isOk());

        verify(albumService).setStickerActive(stickerId, true);
    }

    @Test
    @DisplayName("dado figurinha existente, quando PATCH /admin/stickers/{id}/deactivate, então retorna 200")
    void givenExistingSticker_whenDeactivateSticker_thenReturns200() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        when(albumService.setStickerActive(stickerId, false)).thenReturn(sticker(albumId));

        // Act / Assert
        mockMvc.perform(patch("/admin/stickers/{stickerId}/deactivate", stickerId))
                .andExpect(status().isOk());

        verify(albumService).setStickerActive(stickerId, false);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private AlbumResponse album(UUID id) {
        return new AlbumResponse(id, "Copa 2026", "FIFA 2026", 2026, true,
                LocalDateTime.now(), LocalDateTime.now());
    }

    private StickerResponse sticker(UUID albumId) {
        return new StickerResponse(UUID.randomUUID(), albumId, "001", "Neymar", null, true,
                LocalDateTime.now(), LocalDateTime.now());
    }
}
