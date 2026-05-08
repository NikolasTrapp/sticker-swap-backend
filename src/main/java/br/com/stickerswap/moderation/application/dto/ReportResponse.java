package br.com.stickerswap.moderation.application.dto;

import br.com.stickerswap.moderation.domain.model.ReportReason;
import br.com.stickerswap.moderation.domain.model.ReportStatus;

import java.time.Instant;
import java.util.UUID;

public record ReportResponse(
        UUID reportId,
        UUID reporterId,
        UUID reportedId,
        ReportReason reason,
        String description,
        ReportStatus status,
        Instant createdAt
) {}
