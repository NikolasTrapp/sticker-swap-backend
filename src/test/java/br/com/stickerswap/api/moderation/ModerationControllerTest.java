package br.com.stickerswap.api.moderation;

import br.com.stickerswap.api.moderation.dto.ReportResponse;
import br.com.stickerswap.domain.moderation.model.ReportReason;
import br.com.stickerswap.domain.moderation.model.ReportStatus;
import br.com.stickerswap.domain.moderation.service.ModerationService;
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

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.stickerswap.support.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ModerationControllerTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
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
    @DisplayName("dado usuário autenticado, quando GET /me/blocked-users, então retorna 200 com página")
    void givenAuthenticatedUser_whenListBlockedUsers_thenReturns200() throws Exception {
        // Arrange
        when(moderationService.listBlockedUsers(eq(userId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/me/blocked-users"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("dado usuário autenticado, quando PUT /users/{id}/block, então retorna 204 e delega bloqueio")
    void givenAuthenticatedUser_whenBlockUser_thenReturns204() throws Exception {
        // Arrange
        UUID targetId = UUID.randomUUID();

        // Act / Assert
        mockMvc.perform(put("/users/{userId}/block", targetId))
                .andExpect(status().isNoContent());

        verify(moderationService).blockUser(userId, targetId);
    }

    @Test
    @DisplayName("dado usuário autenticado, quando DELETE /users/{id}/block, então retorna 204 e delega desbloqueio")
    void givenAuthenticatedUser_whenUnblockUser_thenReturns204() throws Exception {
        // Arrange
        UUID targetId = UUID.randomUUID();

        // Act / Assert
        mockMvc.perform(delete("/users/{userId}/block", targetId))
                .andExpect(status().isNoContent());

        verify(moderationService).unblockUser(userId, targetId);
    }

    @Test
    @DisplayName("dado usuário autenticado e motivo válido, quando POST /users/{id}/report, então retorna 201")
    void givenAuthenticatedUserAndValidReason_whenReportUser_thenReturns201() throws Exception {
        // Arrange
        UUID targetId = UUID.randomUUID();
        ReportResponse resp = new ReportResponse(UUID.randomUUID(), userId, targetId, ReportReason.SPAM, null, ReportStatus.PENDING, LocalDateTime.now());
        when(moderationService.reportUser(eq(userId), eq(targetId), any())).thenReturn(resp);

        // Act / Assert
        mockMvc.perform(post("/users/{userId}/report", targetId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"reason\":\"SPAM\"}"))
                .andExpect(status().isCreated());
    }
}
