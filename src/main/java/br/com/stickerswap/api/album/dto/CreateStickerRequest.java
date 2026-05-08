package br.com.stickerswap.api.album.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CreateStickerRequest(
        @NotBlank @Size(max = 20) String number,
        @NotBlank @Size(max = 200) String name,
        String description
) {}
