package br.com.stickerswap.api.admin;

import br.com.stickerswap.api.admin.dto.AdminUserResponse;
import br.com.stickerswap.domain.identity.service.AdminUserService;
import br.com.stickerswap.shared.security.AuthenticatedUser;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/admin/users")
@Tag(name = "Admin — Users")
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
public class AdminUserController {

    private final AdminUserService adminUserService;

    @GetMapping
    @Operation(summary = "List all users", description = "Requires ADMIN role.")
    public Page<AdminUserResponse> listUsers(
            @RequestParam(required = false) String q,
            @PageableDefault(size = 20, sort = "createdAt", direction = Sort.Direction.DESC) Pageable pageable) {
        return adminUserService.listUsers(q, pageable);
    }

    @PatchMapping("/{userId}/block")
    @Operation(summary = "Block a user immediately", description = "Requires ADMIN role. Revokes persisted OAuth sessions and pushes a realtime logout event.")
    public AdminUserResponse blockUser(@PathVariable UUID userId) {
        UUID adminUserId = AuthenticatedUser.fromContext().id();
        return adminUserService.blockUser(adminUserId, userId);
    }

    @PatchMapping("/{userId}/unblock")
    @Operation(summary = "Unblock a user", description = "Requires ADMIN role. The user must sign in again after being unblocked.")
    public AdminUserResponse unblockUser(@PathVariable UUID userId) {
        return adminUserService.unblockUser(userId);
    }
}
