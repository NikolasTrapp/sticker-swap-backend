package br.com.stickerswap.domain.moderation.service;

import br.com.stickerswap.api.moderation.dto.ReportRequest;
import br.com.stickerswap.api.moderation.dto.ReportResponse;
import br.com.stickerswap.domain.moderation.model.ReportStatus;
import br.com.stickerswap.domain.moderation.model.UserBlock;
import br.com.stickerswap.domain.moderation.model.UserReport;
import br.com.stickerswap.infrastructure.repository.moderation.UserBlockRepository;
import br.com.stickerswap.infrastructure.repository.moderation.UserReportRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ModerationServiceImpl implements ModerationService {

    private final UserBlockRepository blockRepo;
    private final UserReportRepository reportRepo;

    @Transactional
    @Override
    public void blockUser(UUID blockerId, UUID targetId) {
        if (blockerId.equals(targetId)) {
            throw new BusinessRuleException("Você não pode bloquear a si mesmo.");
        }
        if (!blockRepo.existsByBlockerIdAndBlockedId(blockerId, targetId)) {
            UserBlock block = new UserBlock();
            block.setBlockerId(blockerId);
            block.setBlockedId(targetId);
            blockRepo.save(block);
        }
    }

    @Transactional
    @Override
    public void unblockUser(UUID blockerId, UUID targetId) {
        if (!blockRepo.existsByBlockerIdAndBlockedId(blockerId, targetId)) {
            throw new ResourceNotFoundException("Block", targetId);
        }
        blockRepo.deleteByBlockerIdAndBlockedId(blockerId, targetId);
    }

    @Transactional
    @Override
    public ReportResponse reportUser(UUID reporterId, UUID reportedId, ReportRequest req) {
        if (reporterId.equals(reportedId)) {
            throw new BusinessRuleException("Você não pode denunciar a si mesmo.");
        }
        UserReport report = new UserReport();
        report.setReporterId(reporterId);
        report.setReportedId(reportedId);
        report.setReason(req.reason());
        report.setDescription(req.description());
        report = reportRepo.save(report);
        return toResponse(report);
    }

    /** Returns all user IDs that have any block relationship with the given user (either direction). */
    @Transactional(readOnly = true)
    @Override
    public Set<UUID> getMutuallyBlockedIds(UUID userId) {
        Set<UUID> blocked = new HashSet<>(blockRepo.findBlockedIdsByBlockerId(userId));
        blocked.addAll(blockRepo.findBlockerIdsByBlockedId(userId));
        return blocked;
    }

    /** Returns true if either user has blocked the other. */
    @Transactional(readOnly = true)
    @Override
    public boolean isBlocked(UUID userA, UUID userB) {
        return blockRepo.existsByBlockerIdAndBlockedId(userA, userB)
            || blockRepo.existsByBlockerIdAndBlockedId(userB, userA);
    }

    @Transactional(readOnly = true)
    @Override
    public Page<ReportResponse> listReports(ReportStatus status, Pageable pageable) {
        Page<UserReport> page = status != null
                ? reportRepo.findByStatus(status, pageable)
                : reportRepo.findAll(pageable);
        return page.map(this::toResponse);
    }

    private ReportResponse toResponse(UserReport r) {
        return new ReportResponse(r.getId(), r.getReporterId(), r.getReportedId(),
                r.getReason(), r.getDescription(), r.getStatus(), r.getCreatedAt());
    }
}
