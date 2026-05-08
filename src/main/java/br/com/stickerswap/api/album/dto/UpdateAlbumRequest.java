package br.com.stickerswap.api.album.dto;

import jakarta.validation.constraints.Size;

public record UpdateAlbumRequest(
        @Size(min = 1, max = 200) String name,
        String description,
        Integer year
) {}
