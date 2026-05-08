package br.com.stickerswap.api.collection.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;

public record SetRepeatedStickerRequest(
        @NotNull @Min(value = 0, message = "quantity must be zero or positive") Integer quantity
) {}
