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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.stickerswap.support.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AlbumControllerTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AlbumService albumService;

    @Test
    @DisplayName("dado usuário autenticado, quando GET /albums, então retorna 200 com página de álbuns")
    void givenAuthenticatedUser_whenListAlbums_thenReturns200WithPage() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        when(albumService.listActiveAlbums(any(Pageable.class))).thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/albums")
                        .with(jwt().jwt(j -> j.subject(userId.toString())
                                .claim("email", "u@e.com").claim("role", "USER"))))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado álbum existente, quando GET /albums/{albumId}, então retorna 200 com AlbumResponse")
    void givenExistingAlbum_whenGetAlbum_thenReturns200() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        AlbumResponse album = new AlbumResponse(albumId, "Copa 2026", "FIFA World Cup 2026", 2026, true,
                LocalDateTime.now(), LocalDateTime.now());
        when(albumService.getActiveAlbum(albumId)).thenReturn(album);

        // Act / Assert
        mockMvc.perform(get("/albums/{albumId}", albumId)
                        .with(jwt().jwt(j -> j.subject(userId.toString())
                                .claim("email", "u@e.com").claim("role", "USER"))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.name").value("Copa 2026"));
    }

    @Test
    @DisplayName("dado álbum existente, quando GET /albums/{albumId}/stickers, então retorna 200 com página de figurinhas")
    void givenExistingAlbum_whenListStickers_thenReturns200WithPage() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID albumId = UUID.randomUUID();
        when(albumService.listActiveStickers(eq(albumId), any(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/albums/{albumId}/stickers", albumId)
                        .with(jwt().jwt(j -> j.subject(userId.toString())
                                .claim("email", "u@e.com").claim("role", "USER"))))
                .andExpect(status().isOk());
    }
}
