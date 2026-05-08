package br.com.stickerswap.api.collection;

import br.com.stickerswap.api.collection.dto.RepeatedStickerResponse;
import br.com.stickerswap.api.collection.dto.SetRepeatedStickerRequest;
import br.com.stickerswap.api.collection.dto.WantedStickerResponse;
import br.com.stickerswap.domain.collection.service.CollectionService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@Tag(name = "Collection", description = "User's repeated and wanted stickers")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class CollectionController {

    private final CollectionService collectionService;

    // ── Repeated ─────────────────────────────────────────────────────────────

    @GetMapping("/me/albums/{albumId}/repeated-stickers")
    @Operation(summary = "List own repeated stickers for an album")
    public List<RepeatedStickerResponse> listRepeated(@PathVariable UUID albumId) {
        return collectionService.listRepeated(AuthenticatedUser.fromContext().id(), albumId);
    }

    @PutMapping("/me/repeated-stickers/{stickerId}")
    @Operation(summary = "Set quantity for a repeated sticker (upsert)")
    public RepeatedStickerResponse setRepeated(@PathVariable UUID stickerId,
                                               @Valid @RequestBody SetRepeatedStickerRequest request) {
        return collectionService.setRepeated(AuthenticatedUser.fromContext().id(), stickerId, request);
    }

    @DeleteMapping("/me/repeated-stickers/{stickerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a sticker from repeated list")
    public void deleteRepeated(@PathVariable UUID stickerId) {
        collectionService.deleteRepeated(AuthenticatedUser.fromContext().id(), stickerId);
    }

    // ── Wanted ───────────────────────────────────────────────────────────────

    @GetMapping("/me/albums/{albumId}/wanted-stickers")
    @Operation(summary = "List own wanted stickers for an album")
    public List<WantedStickerResponse> listWanted(@PathVariable UUID albumId) {
        return collectionService.listWanted(AuthenticatedUser.fromContext().id(), albumId);
    }

    @PutMapping("/me/wanted-stickers/{stickerId}")
    @Operation(summary = "Add a sticker to wanted list (upsert)")
    public WantedStickerResponse setWanted(@PathVariable UUID stickerId) {
        return collectionService.setWanted(AuthenticatedUser.fromContext().id(), stickerId);
    }

    @DeleteMapping("/me/wanted-stickers/{stickerId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Remove a sticker from wanted list")
    public void deleteWanted(@PathVariable UUID stickerId) {
        collectionService.deleteWanted(AuthenticatedUser.fromContext().id(), stickerId);
    }
}
