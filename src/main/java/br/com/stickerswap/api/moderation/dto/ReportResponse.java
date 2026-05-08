package br.com.stickerswap.api.moderation.dto;

import br.com.stickerswap.domain.moderation.model.ReportReason;
import br.com.stickerswap.domain.moderation.model.ReportStatus;

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
