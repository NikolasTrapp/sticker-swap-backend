package br.com.stickerswap.infrastructure.repository.moderation;

import br.com.stickerswap.domain.moderation.model.UserBlock;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.UUID;

public interface UserBlockRepository extends JpaRepository<UserBlock, UUID> {

    boolean existsByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    void deleteByBlockerIdAndBlockedId(UUID blockerId, UUID blockedId);

    Page<UserBlock> findByBlockerIdOrderByCreatedAtDesc(UUID blockerId, Pageable pageable);

    @Query("SELECT b.blockedId FROM UserBlock b WHERE b.blockerId = :userId")
    List<UUID> findBlockedIdsByBlockerId(@Param("userId") UUID userId);

    @Query("SELECT b.blockerId FROM UserBlock b WHERE b.blockedId = :userId")
    List<UUID> findBlockerIdsByBlockedId(@Param("userId") UUID userId);
}
