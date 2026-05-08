package br.com.stickerswap.api.chat;

import br.com.stickerswap.api.chat.dto.ConversationResponse;
import br.com.stickerswap.api.chat.dto.InterestRequest;
import br.com.stickerswap.api.chat.dto.MessageResponse;
import br.com.stickerswap.domain.chat.service.ChatService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Chat", description = "Conversations and messages for trade interest")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ChatController {

    private final ChatService chatService;

    @PostMapping("/stickers/{stickerId}/interest")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Express interest in a sticker held by another user",
               description = "Creates or retrieves an existing conversation. Injects a SYSTEM_INTENT message on first contact.")
    public ConversationResponse expressInterest(
            @PathVariable UUID stickerId,
            @RequestBody @Valid InterestRequest req) {
        UUID seekerId = AuthenticatedUser.fromContext().id();
        return chatService.expressInterest(seekerId, stickerId, req.holderId());
    }

    @GetMapping("/chats")
    @Operation(summary = "List all conversations for the authenticated user",
               description = "Ordered by most recently updated first.")
    public List<ConversationResponse> listConversations() {
        UUID userId = AuthenticatedUser.fromContext().id();
        return chatService.listConversations(userId);
    }

    @GetMapping("/chats/{conversationId}/messages")
    @Operation(summary = "List messages in a conversation",
               description = "Ordered oldest-first. Only participants can access.")
    public Page<MessageResponse> listMessages(
            @PathVariable UUID conversationId,
            @PageableDefault(size = 50) Pageable pageable) {
        UUID userId = AuthenticatedUser.fromContext().id();
        return chatService.listMessages(userId, conversationId, pageable);
    }
}
