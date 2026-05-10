package br.com.stickerswap.api.moderation;

import br.com.stickerswap.api.moderation.dto.BlockedUserResponse;
import br.com.stickerswap.api.moderation.dto.ReportRequest;
import br.com.stickerswap.api.moderation.dto.ReportResponse;
import br.com.stickerswap.domain.moderation.service.ModerationService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@Tag(name = "Moderation", description = "Block and report users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class ModerationController {

    private final ModerationService moderationService;

    @GetMapping("/me/blocked-users")
    @Operation(summary = "List users blocked by the current user")
    public Page<BlockedUserResponse> listBlockedUsers(Pageable pageable) {
        UUID callerId = AuthenticatedUser.fromContext().id();
        return moderationService.listBlockedUsers(callerId, pageable);
    }

    @PutMapping("/users/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Block a user",
               description = "Idempotent. Blocked users are hidden from each other's searches.")
    public void blockUser(@PathVariable UUID userId) {
        UUID callerId = AuthenticatedUser.fromContext().id();
        moderationService.blockUser(callerId, userId);
    }

    @DeleteMapping("/users/{userId}/block")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Unblock a user")
    public void unblockUser(@PathVariable UUID userId) {
        UUID callerId = AuthenticatedUser.fromContext().id();
        moderationService.unblockUser(callerId, userId);
    }

    @PostMapping("/users/{userId}/report")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Report a user")
    public ReportResponse reportUser(
            @PathVariable UUID userId,
            @RequestBody @Valid ReportRequest req) {
        UUID callerId = AuthenticatedUser.fromContext().id();
        return moderationService.reportUser(callerId, userId, req);
    }
}
