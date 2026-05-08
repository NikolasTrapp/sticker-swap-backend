package br.com.stickerswap.api.album;

import br.com.stickerswap.api.album.dto.*;
import br.com.stickerswap.domain.album.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/admin")
@Tag(name = "Admin — Catalog", description = "Album and sticker management (ADMIN only)")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminAlbumController {

    private final AlbumService albumService;

    // ── Albums ───────────────────────────────────────────────────────────────

    @GetMapping("/albums")
    @Operation(summary = "List all albums, including inactive")
    public Page<AlbumResponse> listAlbums(@PageableDefault(size = 50) Pageable pageable) {
        return albumService.listAlbums(pageable);
    }

    @PostMapping("/albums")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a new album")
    public AlbumResponse createAlbum(@Valid @RequestBody CreateAlbumRequest request) {
        return albumService.createAlbum(request);
    }

    @PutMapping("/albums/{albumId}")
    @Operation(summary = "Update album metadata")
    public AlbumResponse updateAlbum(@PathVariable UUID albumId,
                                     @Valid @RequestBody UpdateAlbumRequest request) {
        return albumService.updateAlbum(albumId, request);
    }

    @PatchMapping("/albums/{albumId}/activate")
    @Operation(summary = "Activate an album")
    public AlbumResponse activateAlbum(@PathVariable UUID albumId) {
        return albumService.setAlbumActive(albumId, true);
    }

    @PatchMapping("/albums/{albumId}/deactivate")
    @Operation(summary = "Deactivate an album")
    public AlbumResponse deactivateAlbum(@PathVariable UUID albumId) {
        return albumService.setAlbumActive(albumId, false);
    }

    // ── Stickers ─────────────────────────────────────────────────────────────

    @GetMapping("/albums/{albumId}/stickers")
    @Operation(summary = "List all stickers in an album, including inactive")
    public Page<StickerResponse> listStickers(@PathVariable UUID albumId,
                                              @PageableDefault(size = 200, sort = "number") Pageable pageable) {
        return albumService.listStickers(albumId, pageable);
    }

    @PostMapping("/albums/{albumId}/stickers")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Create a sticker in an album")
    public StickerResponse createSticker(@PathVariable UUID albumId,
                                         @Valid @RequestBody CreateStickerRequest request) {
        return albumService.createSticker(albumId, request);
    }

    @PutMapping("/stickers/{stickerId}")
    @Operation(summary = "Update sticker metadata")
    public StickerResponse updateSticker(@PathVariable UUID stickerId,
                                         @Valid @RequestBody UpdateStickerRequest request) {
        return albumService.updateSticker(stickerId, request);
    }

    @PatchMapping("/stickers/{stickerId}/activate")
    @Operation(summary = "Activate a sticker")
    public StickerResponse activateSticker(@PathVariable UUID stickerId) {
        return albumService.setStickerActive(stickerId, true);
    }

    @PatchMapping("/stickers/{stickerId}/deactivate")
    @Operation(summary = "Deactivate a sticker")
    public StickerResponse deactivateSticker(@PathVariable UUID stickerId) {
        return albumService.setStickerActive(stickerId, false);
    }
}
