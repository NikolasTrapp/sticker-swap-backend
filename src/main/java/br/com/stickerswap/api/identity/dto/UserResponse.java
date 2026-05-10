package br.com.stickerswap.api.identity.dto;

import br.com.stickerswap.domain.identity.model.User;

import java.time.LocalDateTime;
import java.util.UUID;

public record UserResponse(
        UUID id,
        String email,
        String role,
        String status,
        boolean emailVerified,
        LocalDateTime createdAt
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
