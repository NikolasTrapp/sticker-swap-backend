package br.com.stickerswap.identity.application.dto;

import br.com.stickerswap.identity.domain.model.User;

import java.time.Instant;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String role,
        String status,
        boolean emailVerified,
        Instant createdAt
) {
    public static UserResponse from(User user) {
        return new UserResponse(
                user.getId(),
                user.getEmail(),
                user.getRole().name(),
                user.getStatus().name(),
                user.isEmailVerified(),
                user.getCreatedAt()
        );
    }
}
