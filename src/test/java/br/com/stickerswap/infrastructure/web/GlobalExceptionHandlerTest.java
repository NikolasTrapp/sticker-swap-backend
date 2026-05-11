package br.com.stickerswap.infrastructure.web;

import br.com.stickerswap.domain.identity.service.AuthService;
import br.com.stickerswap.domain.collection.service.CollectionService;
import br.com.stickerswap.shared.error.BusinessRuleException;
import br.com.stickerswap.shared.error.EmailAlreadyExistsException;
import br.com.stickerswap.shared.error.RateLimitExceededException;
import br.com.stickerswap.shared.error.ResourceNotFoundException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.data.domain.Pageable;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.jwt;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class GlobalExceptionHandlerTest {

    @Autowired MockMvc mockMvc;

    @MockitoBean AuthService authService;
    @MockitoBean CollectionService collectionService;

    // ── Validation ────────────────────────────────────────────────────────────

    @Test
    @DisplayName("dado corpo inválido (MethodArgumentNotValidException), então retorna 400 com lista de erros de campo")
    void givenInvalidRequestBody_whenPost_thenReturns400WithFieldErrors() throws Exception {
        // Arrange / Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"not-an-email\",\"password\":\"x\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.fieldErrors").isArray());
    }

    // ── EmailAlreadyExistsException → 409 ─────────────────────────────────────

    @Test
    @DisplayName("dado EmailAlreadyExistsException, então retorna 409 Conflict")
    void givenEmailAlreadyExistsException_whenHandled_thenReturns409() throws Exception {
        // Arrange
        when(authService.register(any())).thenThrow(new EmailAlreadyExistsException("user@test.com"));

        // Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.status").value(409));
    }

    // ── BusinessRuleException → 422 ───────────────────────────────────────────

    @Test
    @DisplayName("dado BusinessRuleException, então retorna 422 Unprocessable Entity")
    void givenBusinessRuleException_whenHandled_thenReturns422() throws Exception {
        // Arrange
        when(authService.register(any())).thenThrow(new BusinessRuleException("rule broken"));

        // Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422))
                .andExpect(jsonPath("$.message").value("rule broken"));
    }

    // ── ResourceNotFoundException → 404 ──────────────────────────────────────

    @Test
    @DisplayName("dado ResourceNotFoundException, então retorna 404 Not Found")
    void givenResourceNotFoundException_whenHandled_thenReturns404() throws Exception {
        // Arrange
        doThrow(new ResourceNotFoundException("User", "missing@example.com"))
                .when(authService).requestPasswordReset("missing@example.com");

        // Act / Assert
        mockMvc.perform(post("/auth/password-reset-requests")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"missing@example.com\"}"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── BadCredentialsException → 401 ────────────────────────────────────────

    @Test
    @DisplayName("dado BadCredentialsException, então retorna 401 Unauthorized")
    void givenBadCredentialsException_whenHandled_thenReturns401() throws Exception {
        // Arrange
        when(authService.register(any())).thenThrow(new BadCredentialsException("wrong password"));

        // Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    // ── RateLimitExceededException → 429 ─────────────────────────────────────

    @Test
    @DisplayName("dado RateLimitExceededException, então retorna 429 Too Many Requests")
    void givenRateLimitExceededException_whenHandled_thenReturns429() throws Exception {
        // Arrange
        when(authService.register(any())).thenThrow(new RateLimitExceededException("limit exceeded"));

        // Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.status").value(429));
    }

    // ── HttpRequestMethodNotSupportedException → 405 ─────────────────────────

    @Test
    @DisplayName("dado método HTTP não suportado, então retorna 405 Method Not Allowed")
    void givenWrongHttpMethod_whenHandled_thenReturns405() throws Exception {
        // Arrange / Act / Assert
        mockMvc.perform(delete("/auth/register"))
                .andExpect(status().isMethodNotAllowed())
                .andExpect(jsonPath("$.status").value(405));
    }

    // ── NoResourceFoundException → 404 ───────────────────────────────────────

    @Test
    @DisplayName("dado caminho inexistente, então retorna 404 Not Found")
    void givenUnknownPath_whenHandled_thenReturns404() throws Exception {
        // Arrange / Act / Assert
        mockMvc.perform(get("/completely-unknown-path-xyz-abc"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ── Generic Exception → 500 ───────────────────────────────────────────────

    @Test
    @DisplayName("dado exception genérica não tratada, então retorna 500 Internal Server Error")
    void givenGenericException_whenHandled_thenReturns500() throws Exception {
        // Arrange
        when(authService.register(any())).thenThrow(new RuntimeException("unexpected error"));

        // Act / Assert
        mockMvc.perform(post("/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"email\":\"user@test.com\",\"password\":\"secret123\"}"))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status").value(500));
    }

    // ── CollectionService for constraint violation test ───────────────────────

    @Test
    @DisplayName("dado quantidade inválida em ConstraintViolation, então retorna 400 com erros de campo")
    void givenConstraintViolation_whenHandled_thenReturns400WithFieldErrors() throws Exception {
        // Arrange
        UUID userId = UUID.randomUUID();
        UUID stickerId = UUID.randomUUID();

        // Act / Assert — quantity < 0 triggers @Min validation on SetRepeatedStickerRequest
        mockMvc.perform(put("/me/repeated-stickers/{stickerId}", stickerId)
                        .with(jwt().jwt(j -> j.subject(userId.toString())
                                .claim("email", "u@e.com").claim("role", "USER")))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"quantity\":-5}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400));
    }
}
