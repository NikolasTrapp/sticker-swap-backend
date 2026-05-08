package br.com.stickerswap.api.identity.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record PasswordResetRequest(
        @NotBlank String token,
        @NotBlank @Size(min = 8, message = "must be at least 8 characters") String newPassword
) {}
