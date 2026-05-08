package br.com.stickerswap.moderation.infrastructure.web;

import br.com.stickerswap.moderation.application.dto.ReportResponse;
import br.com.stickerswap.moderation.domain.model.ReportStatus;
import br.com.stickerswap.moderation.application.service.ModerationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/admin/moderation")
@Tag(name = "Admin — Moderation")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminModerationController {

    private final ModerationService moderationService;

    @GetMapping("/reports")
    @Operation(summary = "List user reports", description = "Filter by status. Requires ADMIN role.")
    public Page<ReportResponse> listReports(
            @RequestParam(required = false) ReportStatus status,
            @PageableDefault(size = 20) Pageable pageable) {
        return moderationService.listReports(status, pageable);
    }
}
