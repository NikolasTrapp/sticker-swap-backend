package br.com.stickerswap.moderation.application.dto;

import br.com.stickerswap.moderation.domain.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull ReportReason reason,
        @Size(max = 1000) String description
) {}
