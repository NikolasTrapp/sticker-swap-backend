package br.com.stickerswap.api.moderation;

import br.com.stickerswap.domain.moderation.model.ReportStatus;
import br.com.stickerswap.domain.moderation.service.ModerationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import br.com.stickerswap.support.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AdminModerationControllerTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ModerationService moderationService;

    @Test
    @DisplayName("dado requisição sem filtro, quando GET /admin/moderation/reports, então retorna 200 com todos os reports")
    void givenNoStatusFilter_whenListReports_thenReturns200WithAllReports() throws Exception {
        // Arrange
        when(moderationService.listReports(isNull(), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/admin/moderation/reports"))
                .andExpect(status().isOk());

        verify(moderationService).listReports(isNull(), any(Pageable.class));
    }

    @Test
    @DisplayName("dado filtro de status PENDING, quando GET /admin/moderation/reports?status=PENDING, então filtra por status")
    void givenPendingStatusFilter_whenListReports_thenDelegatesToServiceWithStatus() throws Exception {
        // Arrange
        when(moderationService.listReports(any(ReportStatus.class), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of()));

        // Act / Assert
        mockMvc.perform(get("/admin/moderation/reports").param("status", "PENDING"))
                .andExpect(status().isOk());

        verify(moderationService).listReports(eq(ReportStatus.PENDING), any(Pageable.class));
    }
}
