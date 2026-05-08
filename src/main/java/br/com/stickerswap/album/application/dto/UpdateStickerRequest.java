package br.com.stickerswap.album.application.dto;

import jakarta.validation.constraints.Size;

public record UpdateStickerRequest(
        @Size(min = 1, max = 20) String number,
        @Size(min = 1, max = 200) String name,
        String description
) {}
