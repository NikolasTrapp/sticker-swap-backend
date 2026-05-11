package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.api.admin.dto.AdminUserResponse;
import br.com.stickerswap.domain.identity.event.UserAccessRevokedEvent;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AdminUserServiceImpl implements AdminUserService {

    private final UserRepository userRepository;
    private final JdbcTemplate jdbcTemplate;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    @Override
    public Page<AdminUserResponse> listUsers(String q, Pageable pageable) {
        String query = q == null ? "" : q.trim();
        Page<User> users = query.isBlank()
                ? userRepository.findAll(pageable)
                : userRepository.findByEmailContainingIgnoreCase(query, pageable);
        return users.map(AdminUserResponse::from);
    }

    @Transactional
    @Override
    public AdminUserResponse blockUser(UUID adminUserId, UUID targetUserId) {
        if (adminUserId.equals(targetUserId)) {
            throw new BusinessRuleException("Você não pode bloquear sua própria conta administrativa.");
        }

        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        if (user.getStatus() != UserStatus.INACTIVE) {
            user.setStatus(UserStatus.INACTIVE);
            user = userRepository.save(user);
        }

        jdbcTemplate.update("DELETE FROM oauth2_authorization WHERE principal_name = ?", user.getEmail());
        eventPublisher.publishEvent(new UserAccessRevokedEvent(user.getId(), "ACCOUNT_BLOCKED"));

        return AdminUserResponse.from(user);
    }

    @Transactional
    @Override
    public AdminUserResponse unblockUser(UUID targetUserId) {
        User user = userRepository.findById(targetUserId)
                .orElseThrow(() -> new ResourceNotFoundException("User", targetUserId));

        if (user.getStatus() != UserStatus.ACTIVE) {
            user.setStatus(UserStatus.ACTIVE);
            user = userRepository.save(user);
        }

        return AdminUserResponse.from(user);
    }
}
