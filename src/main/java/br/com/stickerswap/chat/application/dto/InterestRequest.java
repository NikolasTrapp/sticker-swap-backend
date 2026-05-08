package br.com.stickerswap.chat.application.dto;

import jakarta.validation.constraints.NotNull;

import java.util.UUID;

public record InterestRequest(@NotNull UUID holderId) {}
