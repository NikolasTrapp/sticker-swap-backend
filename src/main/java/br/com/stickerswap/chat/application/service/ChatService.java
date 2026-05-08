package br.com.stickerswap.chat.application.service;

import br.com.stickerswap.chat.application.dto.ConversationResponse;
import br.com.stickerswap.chat.application.dto.MessageResponse;
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
