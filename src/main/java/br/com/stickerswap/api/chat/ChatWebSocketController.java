package br.com.stickerswap.api.chat;

import br.com.stickerswap.api.chat.dto.MessageResponse;
import br.com.stickerswap.api.chat.dto.SendMessageRequest;
import br.com.stickerswap.domain.chat.service.ChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.messaging.handler.annotation.DestinationVariable;
import org.springframework.messaging.handler.annotation.MessageMapping;
import org.springframework.messaging.handler.annotation.Payload;
import org.springframework.messaging.simp.SimpMessageHeaderAccessor;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Controller;

import java.util.UUID;

@Controller
@RequiredArgsConstructor
public class ChatWebSocketController {

    private final ChatService chatService;
    private final SimpMessagingTemplate messagingTemplate;

    @MessageMapping("/chat/{conversationId}/send")
    public void sendMessage(
            @DestinationVariable UUID conversationId,
            @Payload @Valid SendMessageRequest request,
            SimpMessageHeaderAccessor headerAccessor) {

        UUID senderId = (UUID) headerAccessor.getSessionAttributes().get("userId");
        MessageResponse response = chatService.sendMessage(senderId, conversationId, request.body());
        messagingTemplate.convertAndSend("/topic/chat/" + conversationId, response);
    }
}
