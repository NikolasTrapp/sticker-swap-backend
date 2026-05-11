package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class UserDetailsServiceImplTest {

    @Mock UserRepository userRepository;

    @InjectMocks UserDetailsServiceImpl userDetailsService;

    @Test
    @DisplayName("dado usuário ativo com email verificado, quando carregar por username, então retorna UserDetails habilitado")
    void givenActiveVerifiedUser_whenLoadByUsername_thenReturnsEnabledUserDetails() {
        // Arrange
        User user = user(UserStatus.ACTIVE, true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("user@example.com");

        // Assert
        assertThat(result.isEnabled()).isTrue();
        assertThat(result.getUsername()).isEqualTo("user@example.com");
        assertThat(result.getAuthorities()).anyMatch(a -> a.getAuthority().equals("ROLE_USER"));
    }

    @Test
    @DisplayName("dado usuário inativo, quando carregar por username, então retorna UserDetails desabilitado")
    void givenInactiveUser_whenLoadByUsername_thenReturnsDisabledUserDetails() {
        // Arrange
        User user = user(UserStatus.INACTIVE, true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("user@example.com");

        // Assert
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("dado usuário com email não verificado, quando carregar por username, então retorna UserDetails desabilitado")
    void givenUnverifiedUser_whenLoadByUsername_thenReturnsDisabledUserDetails() {
        // Arrange
        User user = user(UserStatus.ACTIVE, false);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("user@example.com");

        // Assert
        assertThat(result.isEnabled()).isFalse();
    }

    @Test
    @DisplayName("dado email desconhecido, quando carregar por username, então lança UsernameNotFoundException")
    void givenUnknownEmail_whenLoadByUsername_thenThrowsUsernameNotFoundException() {
        // Arrange
        when(userRepository.findByEmail("missing@example.com")).thenReturn(Optional.empty());

        // Act & Assert
        assertThatThrownBy(() -> userDetailsService.loadUserByUsername("missing@example.com"))
                .isInstanceOf(UsernameNotFoundException.class);
    }

    @Test
    @DisplayName("dado email em maiúsculas, quando carregar por username, então normaliza para minúsculas antes de buscar")
    void givenUppercaseEmail_whenLoadByUsername_thenNormalizesToLowercase() {
        // Arrange
        User user = user(UserStatus.ACTIVE, true);
        when(userRepository.findByEmail("user@example.com")).thenReturn(Optional.of(user));

        // Act
        UserDetails result = userDetailsService.loadUserByUsername("USER@EXAMPLE.COM");

        // Assert
        assertThat(result.getUsername()).isEqualTo("user@example.com");
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User user(UserStatus status, boolean emailVerified) {
        User u = new User();
        u.setEmail("user@example.com");
        u.setPasswordHash("hash");
        u.setRole(UserRole.USER);
        u.setStatus(status);
        u.setEmailVerified(emailVerified);
        return u;
    }
}
