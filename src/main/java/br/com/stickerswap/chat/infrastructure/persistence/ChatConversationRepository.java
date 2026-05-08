package br.com.stickerswap.chat.infrastructure.persistence;

import br.com.stickerswap.chat.domain.model.ChatConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

public interface ChatConversationRepository extends JpaRepository<ChatConversation, UUID> {

    Optional<ChatConversation> findByUserAIdAndUserBIdAndStickerId(UUID userAId, UUID userBId, UUID stickerId);

    @Query("SELECT c FROM ChatConversation c WHERE c.userAId = :userId OR c.userBId = :userId ORDER BY c.updatedAt DESC")
    List<ChatConversation> findAllForUser(@Param("userId") UUID userId);
}
