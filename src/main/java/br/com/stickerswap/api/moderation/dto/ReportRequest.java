package br.com.stickerswap.api.moderation.dto;

import br.com.stickerswap.domain.moderation.model.ReportReason;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record ReportRequest(
        @NotNull ReportReason reason,
        @Size(max = 1000) String description
) {}
