package br.com.stickerswap.api.collection;

import br.com.stickerswap.api.collection.dto.CollectionFilter;
import br.com.stickerswap.api.collection.dto.CollectionStickerResponse;
import br.com.stickerswap.api.collection.dto.RepeatedStickerResponse;
import br.com.stickerswap.api.collection.dto.WantedStickerResponse;
import br.com.stickerswap.domain.collection.service.CollectionService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class CollectionControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean CollectionService collectionService;

    private UUID userId;
    private MockedStatic<AuthenticatedUser> mockedAuth;

    @BeforeEach
    void setUp() {
        userId = UUID.randomUUID();
        AuthenticatedUser auth = new AuthenticatedUser(userId, "u@e.com", "USER");
        mockedAuth = mockStatic(AuthenticatedUser.class);
        mockedAuth.when(AuthenticatedUser::fromContext).thenReturn(auth);
    }

    @AfterEach
    void tearDown() {
        mockedAuth.close();
    }

    @Test
    @DisplayName("dado usuário autenticado, quando GET /me/albums/{albumId}/collection, então delega ao serviço e retorna 200")
    void givenAuthenticatedUser_whenListCollection_thenDelegatesToServiceAndReturns200() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        when(collectionService.listCollection(eq(userId), eq(albumId), any(), any(CollectionFilter.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/me/albums/{albumId}/collection", albumId))
                .andExpect(status().isOk());

        verify(collectionService).listCollection(eq(userId), eq(albumId), any(), any(), any());
    }

    @Test
    @DisplayName("dado usuário autenticado, quando GET /me/albums/{albumId}/repeated-stickers, então retorna 200")
    void givenAuthenticatedUser_whenListRepeated_thenReturns200() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        when(collectionService.listRepeated(userId, albumId)).thenReturn(List.of());

        // Act / Assert
        mockMvc.perform(get("/me/albums/{albumId}/repeated-stickers", albumId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado usuário autenticado e corpo válido, quando PUT /me/repeated-stickers/{stickerId}, então retorna 200")
    void givenAuthenticatedUserAndValidBody_whenSetRepeated_thenReturns200() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();
        RepeatedStickerResponse resp = new RepeatedStickerResponse(UUID.randomUUID(), stickerId, "001", "Neymar", 3, null);
        when(collectionService.setRepeated(eq(userId), eq(stickerId), any())).thenReturn(resp);

        // Act / Assert
        mockMvc.perform(put("/me/repeated-stickers/{stickerId}", stickerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":3}"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado usuário autenticado, quando DELETE /me/repeated-stickers/{stickerId}, então retorna 204")
    void givenAuthenticatedUser_whenDeleteRepeated_thenReturns204() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();

        // Act / Assert
        mockMvc.perform(delete("/me/repeated-stickers/{stickerId}", stickerId))
                .andExpect(status().isNoContent());

        verify(collectionService).deleteRepeated(userId, stickerId);
    }

    @Test
    @DisplayName("dado usuário autenticado, quando GET /me/albums/{albumId}/wanted-stickers, então retorna 200")
    void givenAuthenticatedUser_whenListWanted_thenReturns200() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        when(collectionService.listWanted(userId, albumId)).thenReturn(List.of());

        // Act / Assert
        mockMvc.perform(get("/me/albums/{albumId}/wanted-stickers", albumId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado usuário autenticado, quando PUT /me/wanted-stickers/{stickerId}, então retorna 200")
    void givenAuthenticatedUser_whenSetWanted_thenReturns200() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();
        WantedStickerResponse resp = new WantedStickerResponse(UUID.randomUUID(), stickerId, "001", "Neymar", null);
        when(collectionService.setWanted(userId, stickerId)).thenReturn(resp);

        // Act / Assert
        mockMvc.perform(put("/me/wanted-stickers/{stickerId}", stickerId))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado usuário autenticado, quando DELETE /me/wanted-stickers/{stickerId}, então retorna 204")
    void givenAuthenticatedUser_whenDeleteWanted_thenReturns204() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();

        // Act / Assert
        mockMvc.perform(delete("/me/wanted-stickers/{stickerId}", stickerId))
                .andExpect(status().isNoContent());

        verify(collectionService).deleteWanted(userId, stickerId);
    }
}
