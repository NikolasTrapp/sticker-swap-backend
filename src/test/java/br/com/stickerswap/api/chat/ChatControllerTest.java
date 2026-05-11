package br.com.stickerswap.api.chat;

import br.com.stickerswap.api.chat.dto.ConversationResponse;
import br.com.stickerswap.api.chat.dto.MessageResponse;
import br.com.stickerswap.domain.chat.model.MessageType;
import br.com.stickerswap.domain.chat.service.ChatService;
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
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.stickerswap.support.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class ChatControllerTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean ChatService chatService;
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
    @DisplayName("dado usuário autenticado e requisição de interesse válida, quando POST /stickers/{id}/interest, então retorna 201")
    void givenValidInterestRequest_whenPost_thenReturns201() throws Exception {
        // Arrange
        UUID stickerId = UUID.randomUUID();
        UUID holderId = UUID.randomUUID();
        ConversationResponse conv = conversation(userId, holderId, stickerId);
        when(chatService.expressInterest(eq(userId), eq(stickerId), eq(holderId))).thenReturn(conv);

        // Act / Assert
        mockMvc.perform(post("/stickers/{stickerId}/interest", stickerId)
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"holderId\":\"" + holderId + "\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.conversationId").value(conv.conversationId().toString()));
    }

    @Test
    @DisplayName("dado usuário autenticado, quando GET /chats, então retorna 200 com lista de conversas")
    void givenAuthenticatedUser_whenListConversations_thenReturns200() throws Exception {
        // Arrange
        UUID otherId = UUID.randomUUID();
        UUID stickerId = UUID.randomUUID();
        when(chatService.listConversations(userId)).thenReturn(List.of(conversation(userId, otherId, stickerId)));

        // Act / Assert
        mockMvc.perform(get("/chats"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$").isArray());
    }

    @Test
    @DisplayName("dado conversa válida, quando GET /chats/{id}/messages, então retorna 200 paginado")
    void givenValidConversationId_whenListMessages_thenReturns200WithPage() throws Exception {
        // Arrange
        UUID convId = UUID.randomUUID();
        MessageResponse msg = new MessageResponse(UUID.randomUUID(), convId, userId, MessageType.TEXT, "oi", LocalDateTime.now());
        when(chatService.listMessages(eq(userId), eq(convId), any(Pageable.class)))
                .thenReturn(new PageImpl<>(List.of(msg)));

        // Act / Assert
        mockMvc.perform(get("/chats/{conversationId}/messages", convId))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.content").isArray());
    }

    @Test
    @DisplayName("dado conversa válida, quando PUT /chats/{id}/notifications/read, então retorna 204 e delega ao serviço")
    void givenValidConversationId_whenMarkConversationRead_thenReturns204() throws Exception {
        // Arrange
        UUID convId = UUID.randomUUID();

        // Act / Assert
        mockMvc.perform(put("/chats/{conversationId}/notifications/read", convId))
                .andExpect(status().isNoContent());

        verify(notificationService).markConversationRead(userId, convId);
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private ConversationResponse conversation(UUID userAId, UUID userBId, UUID stickerId) {
        return new ConversationResponse(UUID.randomUUID(), userBId, "nickname", stickerId, "001", "Neymar",
                LocalDateTime.now(), LocalDateTime.now());
    }
}
