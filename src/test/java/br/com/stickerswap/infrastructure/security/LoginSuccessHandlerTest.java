package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class LoginSuccessHandlerTest {

    @Mock UserRepository userRepository;
    @Mock HttpServletRequest request;
    @Mock HttpServletResponse response;
    @Mock Authentication authentication;

    @InjectMocks LoginSuccessHandler loginSuccessHandler;

    @Test
    @DisplayName("dado usuário conhecido, quando login com sucesso, então atualiza lastActivityAt")
    void givenKnownUser_whenLoginSuccess_thenUpdatesLastActivityAt() throws Exception {
        // Arrange
        User user = activeUser("user@example.com");
        when(authentication.getName()).thenReturn("user@example.com");
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));
        when(userRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        // Act
        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        ArgumentCaptor<User> captor = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(captor.capture());
        assertThat(captor.getValue().getLastActivityAt()).isNotNull();
    }

    @Test
    @DisplayName("dado email desconhecido, quando login com sucesso, então não tenta salvar usuário")
    void givenUnknownUser_whenLoginSuccess_thenNoSaveAttempt() throws Exception {
        // Arrange
        when(authentication.getName()).thenReturn("ghost@example.com");
        when(userRepository.findByEmail("ghost@example.com")).thenReturn(Optional.empty());

        // Act
        loginSuccessHandler.onAuthenticationSuccess(request, response, authentication);

        // Assert
        verify(userRepository, never()).save(any());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User activeUser(String email) {
        User u = new User();
        u.setEmail(email);
        u.setRole(UserRole.USER);
        u.setStatus(UserStatus.ACTIVE);
        u.setEmailVerified(true);
        return u;
    }
}
