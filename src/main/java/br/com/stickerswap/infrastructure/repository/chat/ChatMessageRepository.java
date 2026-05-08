package br.com.stickerswap.infrastructure.repository.chat;

import br.com.stickerswap.domain.chat.model.ChatMessage;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface ChatMessageRepository extends JpaRepository<ChatMessage, UUID> {

    Page<ChatMessage> findByConversationIdOrderBySentAtAsc(UUID conversationId, Pageable pageable);
}
