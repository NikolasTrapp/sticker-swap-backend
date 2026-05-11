package br.com.stickerswap.api.identity;

import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.domain.identity.service.AuthService;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.stickerswap.support.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class AuthControllerTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @MockitoBean AuthService authService;

    // ── POST /auth/register ───────────────────────────────────────────────────

    @Test
    @DisplayName("dado requisição de cadastro válida, quando POST /auth/register, então retorna 201 com UserResponse")
    void givenValidRegisterRequest_whenPost_thenReturns201WithUserResponse() throws Exception {
        // Arrange
        User user = activeUser("user@example.com");
        when(authService.register(any())).thenReturn(user);

        // Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com","password":"secret123"}
                                """))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value("user@example.com"))
                .andExpect(jsonPath("$.emailVerified").value(false));
    }

    @Test
    @DisplayName("dado email inválido, quando POST /auth/register, então retorna 400 com erros de campo")
    void givenInvalidEmail_whenRegister_thenReturns400WithFieldErrors() throws Exception {
        // Arrange / Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"not-an-email","password":"secret123"}
                                """))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    @Test
    @DisplayName("dado corpo vazio, quando POST /auth/register, então retorna 400")
    void givenMissingBody_whenRegister_thenReturns400() throws Exception {
        // Arrange / Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{}"))
                .andExpect(status().isBadRequest());
    }

    // ── POST /auth/email-confirmations ────────────────────────────────────────

    @Test
    @DisplayName("dado email válido, quando POST /auth/email-confirmations, então retorna 204")
    void givenValidEmail_whenResendConfirmation_thenReturns204() throws Exception {
        // Arrange
        doNothing().when(authService).resendEmailConfirmation("user@example.com");

        // Act / Assert
        mockMvc.perform(post("/auth/email-confirmations")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isNoContent());

        verify(authService).resendEmailConfirmation("user@example.com");
    }

    // ── GET /auth/email-confirmations/confirm ─────────────────────────────────

    @Test
    @DisplayName("dado token válido com redirect=false, quando GET /confirm, então retorna 200 com UserResponse JSON")
    void givenValidToken_whenConfirmEmailWithRedirectFalse_thenReturns200WithUserResponse() throws Exception {
        // Arrange
        User user = activeVerifiedUser("user@example.com");
        when(authService.confirmEmail("valid-token")).thenReturn(user);

        // Act / Assert
        mockMvc.perform(get("/auth/email-confirmations/confirm")
                        .param("token", "valid-token")
                        .param("redirect", "false"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));
    }

    @Test
    @DisplayName("dado token válido sem redirect=false, quando GET /confirm, então redireciona para página confirmada do frontend")
    void givenValidToken_whenConfirmEmail_thenRedirectsToFrontend() throws Exception {
        // Arrange
        User user = activeVerifiedUser("user@example.com");
        when(authService.confirmEmail("valid-token")).thenReturn(user);

        // Act / Assert
        mockMvc.perform(get("/auth/email-confirmations/confirm")
                        .param("token", "valid-token"))
                .andExpect(status().isFound())
                .andExpect(header().string("Location", org.hamcrest.Matchers.containsString("email-confirmed")));
    }

    // ── POST /auth/password-reset-requests ───────────────────────────────────

    @Test
    @DisplayName("dado email existente, quando POST /auth/password-reset-requests, então retorna 204")
    void givenExistingEmail_whenPasswordResetRequest_thenReturns204() throws Exception {
        // Arrange
        doNothing().when(authService).requestPasswordReset("user@example.com");

        // Act / Assert
        mockMvc.perform(post("/auth/password-reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"user@example.com"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("dado email desconhecido, quando POST /auth/password-reset-requests, então retorna 404")
    void givenUnknownEmail_whenPasswordResetRequest_thenReturns404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User", "missing@example.com"))
                .when(authService).requestPasswordReset("missing@example.com");

        // Act / Assert
        mockMvc.perform(post("/auth/password-reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"email":"missing@example.com"}
                                """))
                .andExpect(status().isNotFound());
    }

    // ── POST /auth/password-resets ────────────────────────────────────────────

    @Test
    @DisplayName("dado token e nova senha válidos, quando POST /auth/password-resets, então retorna 204")
    void givenValidTokenAndNewPassword_whenPasswordReset_thenReturns204() throws Exception {
        // Arrange
        doNothing().when(authService).resetPassword(eq("valid-token"), eq("new-password-123"));

        // Act / Assert
        mockMvc.perform(post("/auth/password-resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"valid-token","newPassword":"new-password-123"}
                                """))
                .andExpect(status().isNoContent());
    }

    @Test
    @DisplayName("dado token inválido, quando POST /auth/password-resets, então retorna 422")
    void givenInvalidToken_whenPasswordReset_thenReturns422() throws Exception {
        // Arrange
        doThrow(new BusinessRuleException("Invalid or expired token"))
                .when(authService).resetPassword(eq("bad-token"), any());

        // Act / Assert
        mockMvc.perform(post("/auth/password-resets")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {"token":"bad-token","newPassword":"new-password-123"}
                                """))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── helpers ───────────────────────────────────────────────────────────────

    private User activeUser(String email) {
        User u = new User();
        u.setId(UUID.randomUUID());
        u.setEmail(email);
        u.setPasswordHash("hash");
        u.setRole(UserRole.USER);
        u.setStatus(UserStatus.ACTIVE);
        u.setEmailVerified(false);
        u.setCreatedAt(LocalDateTime.now());
        return u;
    }

    private User activeVerifiedUser(String email) {
        User u = activeUser(email);
        u.setEmailVerified(true);
        return u;
    }
}
