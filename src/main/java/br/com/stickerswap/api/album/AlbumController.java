package br.com.stickerswap.api.album;

import br.com.stickerswap.api.album.dto.AlbumResponse;
import br.com.stickerswap.api.album.dto.StickerResponse;
import br.com.stickerswap.domain.album.service.AlbumService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.*;

import java.util.UUID;

@RestController
@RequestMapping("/albums")
@Tag(name = "Albums", description = "Public album and sticker catalog")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AlbumController {

    private final AlbumService albumService;

    @GetMapping
    @Operation(summary = "List active albums")
    public Page<AlbumResponse> listAlbums(@PageableDefault(size = 20) Pageable pageable) {
        return albumService.listActiveAlbums(pageable);
    }

    @GetMapping("/{albumId}")
    @Operation(summary = "Get active album by ID")
    public AlbumResponse getAlbum(@PathVariable UUID albumId) {
        return albumService.getActiveAlbum(albumId);
    }

    @GetMapping("/{albumId}/stickers")
    @Operation(summary = "List active stickers in an album")
    public Page<StickerResponse> listStickers(
            @PathVariable UUID albumId,
            @RequestParam(required = false) String q,
            @PageableDefault(size = 50, sort = "code") Pageable pageable) {
        return albumService.listActiveStickers(albumId, q, pageable);
    }
}
