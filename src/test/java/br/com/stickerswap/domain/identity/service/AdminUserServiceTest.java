package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.domain.identity.event.UserAccessRevokedEvent;
import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.shared.error.BusinessRuleException;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.jdbc.core.JdbcTemplate;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AdminUserServiceTest {

    @Mock UserRepository userRepository;
    @Mock JdbcTemplate jdbcTemplate;
    @Mock ApplicationEventPublisher eventPublisher;

    @InjectMocks AdminUserServiceImpl adminUserService;

    @Test
    void listUsers_filtersByEmail_whenQueryIsPresent() {
        User user = user(UUID.randomUUID(), "user@example.com", UserStatus.ACTIVE);
        PageRequest pageable = PageRequest.of(0, 20);

        when(userRepository.findByEmailContainingIgnoreCase("user@example.com", pageable))
                .thenReturn(new PageImpl<>(List.of(user), pageable, 1));

        var response = adminUserService.listUsers(" user@example.com ", pageable);

        assertThat(response.getTotalElements()).isEqualTo(1);
        assertThat(response.getContent().getFirst().email()).isEqualTo("user@example.com");
    }

    @Test
    void blockUser_inactivatesUserRevokesAuthorizationsAndPublishesRealtimeEvent() {
        UUID adminId = UUID.randomUUID();
        UUID targetId = UUID.randomUUID();
        User user = user(targetId, "blocked@example.com", UserStatus.ACTIVE);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = adminUserService.blockUser(adminId, targetId);

        assertThat(response.status()).isEqualTo("INACTIVE");
        assertThat(user.getStatus()).isEqualTo(UserStatus.INACTIVE);
        verify(jdbcTemplate).update("DELETE FROM oauth2_authorization WHERE principal_name = ?", "blocked@example.com");

        ArgumentCaptor<UserAccessRevokedEvent> event = ArgumentCaptor.forClass(UserAccessRevokedEvent.class);
        verify(eventPublisher).publishEvent(event.capture());
        assertThat(event.getValue().userId()).isEqualTo(targetId);
        assertThat(event.getValue().reason()).isEqualTo("ACCOUNT_BLOCKED");
    }

    @Test
    void blockUser_rejectsSelfBlock() {
        UUID adminId = UUID.randomUUID();

        assertThatThrownBy(() -> adminUserService.blockUser(adminId, adminId))
                .isInstanceOf(BusinessRuleException.class);

        verify(userRepository, never()).findById(any());
    }

    @Test
    void unblockUser_reactivatesUser() {
        UUID targetId = UUID.randomUUID();
        User user = user(targetId, "active-again@example.com", UserStatus.INACTIVE);

        when(userRepository.findById(targetId)).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        var response = adminUserService.unblockUser(targetId);

        assertThat(response.status()).isEqualTo("ACTIVE");
        assertThat(user.getStatus()).isEqualTo(UserStatus.ACTIVE);
    }

    private User user(UUID id, String email, UserStatus status) {
        User user = new User();
        user.setId(id);
        user.setEmail(email);
        user.setStatus(status);
        return user;
    }
}
