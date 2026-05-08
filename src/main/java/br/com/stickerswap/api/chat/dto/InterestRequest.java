package br.com.stickerswap.api.chat.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InterestRequest(@NotNull UUID holderId) {}
