package br.com.stickerswap.api.search;

import br.com.stickerswap.domain.moderation.service.ModerationService;
import br.com.stickerswap.domain.search.service.SearchService;
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
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.Set;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class SearchControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean SearchService searchService;
    @MockitoBean ModerationService moderationService;

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
    @DisplayName("dado stickerId válido, quando GET /albums/{albumId}/stickers/{stickerId}/holders, então delega ao serviço e retorna página")
    void givenStickerId_whenFindHolders_thenDelegatesToServiceAndReturnsPage() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        UUID stickerId = UUID.randomUUID();
        when(moderationService.getMutuallyBlockedIds(userId)).thenReturn(Set.of());
        when(searchService.findHolders(eq(userId), eq(albumId), eq(stickerId), anySet(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/albums/{albumId}/stickers/{stickerId}/holders", albumId, stickerId))
                .andExpect(status().isOk());

        verify(searchService).findHolders(eq(userId), eq(albumId), eq(stickerId), anySet(), any());
    }

    @Test
    @DisplayName("dado usuário com bloqueados, quando buscar holders, então exclui IDs bloqueados da busca")
    void givenUserWithBlockedIds_whenFindHolders_thenPassesBlockedIdsToService() throws Exception {
        // Arrange
        UUID albumId = UUID.randomUUID();
        UUID stickerId = UUID.randomUUID();
        UUID blockedId = UUID.randomUUID();
        when(moderationService.getMutuallyBlockedIds(userId)).thenReturn(Set.of(blockedId));
        when(searchService.findHolders(eq(userId), eq(albumId), eq(stickerId), eq(Set.of(blockedId)), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/albums/{albumId}/stickers/{stickerId}/holders", albumId, stickerId))
                .andExpect(status().isOk());

        verify(searchService).findHolders(eq(userId), eq(albumId), eq(stickerId), eq(Set.of(blockedId)), any(Pageable.class));
    }
}
