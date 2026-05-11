package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.api.admin.dto.AdminUserResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.util.UUID;

public interface AdminUserService {
    Page<AdminUserResponse> listUsers(String q, Pageable pageable);
    AdminUserResponse blockUser(UUID adminUserId, UUID targetUserId);
    AdminUserResponse unblockUser(UUID targetUserId);
}
