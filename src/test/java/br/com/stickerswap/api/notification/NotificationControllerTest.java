package br.com.stickerswap.api.notification;

import br.com.stickerswap.domain.notification.service.NotificationService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.util.UUID;

import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class NotificationControllerTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean NotificationService notificationService;

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
    @DisplayName("dado usuário autenticado, quando GET /notifications, então retorna 200 com lista")
    void givenAuthenticatedUser_whenListNotifications_thenReturns200() throws Exception {
        // Arrange
        when(notificationService.listRecent(userId)).thenReturn(List.of());

        // Act / Assert
        mockMvc.perform(get("/notifications"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("dado usuário autenticado, quando GET /notifications/unread-count, então retorna 200 com contagem")
    void givenAuthenticatedUser_whenUnreadCount_thenReturns200() throws Exception {
        // Arrange
        when(notificationService.countUnread(userId)).thenReturn(5L);

        // Act / Assert
        mockMvc.perform(get("/notifications/unread-count"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.count").value(5));
    }

    @Test
    @DisplayName("dado notificação existente, quando PUT /notifications/{id}/read, então retorna 204")
    void givenExistingNotification_whenMarkAsRead_thenReturns204() throws Exception {
        // Arrange
        UUID notifId = UUID.randomUUID();

        // Act / Assert
        mockMvc.perform(put("/notifications/{notificationId}/read", notifId))
                .andExpect(status().isNoContent());

        verify(notificationService).markAsRead(userId, notifId);
    }

    @Test
    @DisplayName("dado usuário autenticado, quando PUT /notifications/read-all, então retorna 204 e delega ao serviço")
    void givenAuthenticatedUser_whenMarkAllAsRead_thenReturns204() throws Exception {
        // Arrange / Act / Assert
        mockMvc.perform(put("/notifications/read-all"))
                .andExpect(status().isNoContent());

        verify(notificationService).markAllAsRead(userId);
    }
}
