package br.com.stickerswap.domain.chat.service;

import br.com.stickerswap.api.chat.dto.ConversationResponse;
import br.com.stickerswap.api.chat.dto.MessageResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.List;
import java.util.UUID;

public interface ChatService {

    ConversationResponse expressInterest(UUID seekerId, UUID stickerId, UUID holderId);

    List<ConversationResponse> listConversations(UUID userId);

    Page<MessageResponse> listMessages(UUID userId, UUID conversationId, Pageable pageable);

    MessageResponse sendMessage(UUID senderId, UUID conversationId, String body);
}
