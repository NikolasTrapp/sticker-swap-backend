package br.com.stickerswap.api.album.dto;

import jakarta.validation.constraints.Size;

public record UpdateStickerRequest(
        @Size(min = 1, max = 20) String code,
        @Size(min = 1, max = 200) String name,
        String description
) {}
