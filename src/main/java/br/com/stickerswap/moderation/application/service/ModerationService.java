package br.com.stickerswap.moderation.application.service;

import br.com.stickerswap.moderation.application.dto.ReportRequest;
import br.com.stickerswap.moderation.application.dto.ReportResponse;
import br.com.stickerswap.moderation.domain.model.ReportStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.Set;
import java.util.UUID;

public interface ModerationService {

    void blockUser(UUID blockerId, UUID targetId);

    void unblockUser(UUID blockerId, UUID targetId);

    ReportResponse reportUser(UUID reporterId, UUID reportedId, ReportRequest req);

    Set<UUID> getMutuallyBlockedIds(UUID userId);

    boolean isBlocked(UUID userA, UUID userB);

    Page<ReportResponse> listReports(ReportStatus status, Pageable pageable);
}
