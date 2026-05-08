package br.com.stickerswap.search.infrastructure.web;

import br.com.stickerswap.moderation.application.service.ModerationService;
import br.com.stickerswap.search.application.dto.HolderResponse;
import br.com.stickerswap.search.application.service.SearchService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@Tag(name = "Search", description = "Find users who hold a specific sticker")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class SearchController {

    private final SearchService searchService;
    private final ModerationService moderationService;

    @GetMapping("/albums/{albumId}/stickers/{stickerId}/holders")
    @Operation(summary = "List users who have this sticker available for trade",
               description = "Ordered by: same city, potential match, quantity, recent activity. " +
                             "Blocked users are excluded. Approximate location only.")
    public Page<HolderResponse> findHolders(
            @PathVariable UUID albumId,
            @PathVariable UUID stickerId,
            @PageableDefault(size = 20) Pageable pageable) {

        UUID searcherId = AuthenticatedUser.fromContext().id();
        return searchService.findHolders(searcherId, albumId, stickerId,
                moderationService.getMutuallyBlockedIds(searcherId), pageable);
    }
}
