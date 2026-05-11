package br.com.stickerswap.api.admin.dto;

import br.com.stickerswap.domain.identity.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record AdminUserResponse(
        UUID id,
        String email,
        String role,
        String status,
        boolean emailVerified,
        LocalDateTime emailVerifiedAt,
        LocalDateTime createdAt,
        LocalDateTime updatedAt,
        LocalDateTime lastActivityAt,
        String lastIpAddress
) {
    public static AdminUserResponse from(User user) {
        return new AdminUserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.getEmailVerifiedAt(),
                user.getCreatedAt(),
                user.getUpdatedAt(),
                user.getLastActivityAt(),
                user.getLastIpAddress()
        );
    }
}
