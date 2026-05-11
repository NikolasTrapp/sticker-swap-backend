package br.com.stickerswap.api.chat;

import br.com.stickerswap.api.chat.dto.MessageResponse;
import br.com.stickerswap.api.chat.dto.SendMessageRequest;
import br.com.stickerswap.domain.chat.model.MessageType;
import br.com.stickerswap.domain.chat.service.ChatService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ChatWebSocketControllerTest {

    @Mock ChatService chatService;
    @Mock SimpMessagingTemplate messagingTemplate;
    @InjectMocks ChatWebSocketController controller;

    @Test
    @DisplayName("dado mensagem válida de remetente autenticado, quando sendMessage, então publica resposta no tópico da conversa")
    void givenValidMessageFromAuthenticatedSender_whenSendMessage_thenPublishesToConversationTopic() {
        // Arrange
        UUID senderId = UUID.randomUUID();
        UUID conversationId = UUID.randomUUID();
        SendMessageRequest request = new SendMessageRequest("Olá, topa trocar?");
        MessageResponse response = new MessageResponse(UUID.randomUUID(), conversationId, senderId,
                MessageType.TEXT, "Olá, topa trocar?", LocalDateTime.now());

        SimpMessageHeaderAccessor headerAccessor = mock(SimpMessageHeaderAccessor.class);
        Map<String, Object> sessionAttributes = new HashMap<>();
        sessionAttributes.put("userId", senderId);
        when(headerAccessor.getSessionAttributes()).thenReturn(sessionAttributes);
        when(chatService.sendMessage(senderId, conversationId, "Olá, topa trocar?")).thenReturn(response);

        // Act
        controller.sendMessage(conversationId, request, headerAccessor);

        // Assert
        verify(chatService).sendMessage(senderId, conversationId, "Olá, topa trocar?");
        verify(messagingTemplate).convertAndSend("/topic/chat/" + conversationId, response);
    }
}
