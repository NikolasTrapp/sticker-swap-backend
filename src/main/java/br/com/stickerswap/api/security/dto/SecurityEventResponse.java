package br.com.stickerswap.api.security.dto;

public record SecurityEventResponse(
        String type,
        String message
) {}
