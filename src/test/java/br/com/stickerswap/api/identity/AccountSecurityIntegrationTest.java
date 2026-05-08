package br.com.stickerswap.api.identity;

import br.com.stickerswap.domain.identity.service.AccountEmailService;
import br.com.stickerswap.support.PostgresIntegrationTest;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mock.web.MockHttpSession;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.time.Instant;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.reset;
import static org.springframework.http.MediaType.APPLICATION_JSON;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class AccountSecurityIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;
    ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @MockitoBean
    AccountEmailService accountEmailService;

    @Test
    void oauthBrowserLogin_createsAuthenticatedSessionAfterCsrfValidation() throws Exception {
        String email = "oauth-" + UUID.randomUUID() + "@example.com";
        String confirmationToken = registerUserAndCaptureConfirmation(email);

        mockMvc.perform(get("/auth/email-confirmations/confirm")
                        .param("token", confirmationToken))
                .andExpect(status().isOk());

        MvcResult csrfResult = mockMvc.perform(get("/oauth2/csrf"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andReturn();
        String csrfToken = objectMapper.readTree(csrfResult.getResponse().getContentAsString())
                .get("token")
                .asText();
        MvcResult loginResult = mockMvc.perform(post("/oauth2/login")
                        .cookie(csrfResult.getResponse().getCookies())
                        .header("X-XSRF-TOKEN", csrfToken)
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isNoContent())
                .andReturn();

        MockHttpSession session = (MockHttpSession) loginResult.getRequest().getSession(false);
        assertThat(session).isNotNull();
        assertThat(session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)).isNotNull();
        Instant lastActivityAt = jdbcTemplate.queryForObject(
                "SELECT last_activity_at FROM users WHERE email = ?",
                Instant.class,
                email);
        assertThat(lastActivityAt).isNotNull();
    }

    @Test
    void registerAndConfirmEmail_persistsPendingUserAndConsumesConfirmationToken() throws Exception {
        String email = "confirm-" + UUID.randomUUID() + "@example.com";

        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.email").value(email))
                .andExpect(jsonPath("$.emailVerified").value(false));

        Boolean emailVerified = jdbcTemplate.queryForObject(
                "SELECT email_verified FROM users WHERE email = ?",
                Boolean.class,
                email);
        assertThat(emailVerified).isFalse();

        ArgumentCaptor<String> confirmationToken = ArgumentCaptor.forClass(String.class);
        verify(accountEmailService).sendEmailConfirmation(eq(email), confirmationToken.capture());

        mockMvc.perform(get("/auth/email-confirmations/confirm")
                        .param("token", confirmationToken.getValue()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.emailVerified").value(true));

        Integer consumedTokens = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM security_tokens st
                JOIN users u ON u.id = st.user_id
                WHERE u.email = ?
                  AND st.type = 'EMAIL_CONFIRMATION'
                  AND st.consumed_at IS NOT NULL
                """,
                Integer.class,
                email);
        assertThat(consumedTokens).isEqualTo(1);
    }

    @Test
    void passwordReset_generatesTokenAndUpdatesPasswordHashWithoutExposingUnknownEmails() throws Exception {
        String email = "reset-" + UUID.randomUUID() + "@example.com";
        String confirmationToken = registerUserAndCaptureConfirmation(email);

        mockMvc.perform(get("/auth/email-confirmations/confirm")
                        .param("token", confirmationToken))
                .andExpect(status().isOk());

        String oldHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?",
                String.class,
                email);

        reset(accountEmailService);
        mockMvc.perform(post("/auth/password-reset-requests")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s"}
                                """.formatted(email)))
                .andExpect(status().isNoContent());

        ArgumentCaptor<String> resetToken = ArgumentCaptor.forClass(String.class);
        verify(accountEmailService).sendPasswordReset(eq(email), resetToken.capture());

        mockMvc.perform(post("/auth/password-resets")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"token":"%s","newPassword":"new-secret-123"}
                                """.formatted(resetToken.getValue())))
                .andExpect(status().isNoContent());

        String newHash = jdbcTemplate.queryForObject(
                "SELECT password_hash FROM users WHERE email = ?",
                String.class,
                email);
        assertThat(newHash).isNotEqualTo(oldHash);

        Integer consumedTokens = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM security_tokens st
                JOIN users u ON u.id = st.user_id
                WHERE u.email = ?
                  AND st.type = 'PASSWORD_RESET'
                  AND st.consumed_at IS NOT NULL
                """,
                Integer.class,
                email);
        assertThat(consumedTokens).isEqualTo(1);

        reset(accountEmailService);
        mockMvc.perform(post("/auth/password-reset-requests")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"missing-%s@example.com"}
                                """.formatted(UUID.randomUUID())))
                .andExpect(status().isNoContent());
        verifyNoInteractions(accountEmailService);
    }

    private String registerUserAndCaptureConfirmation(String email) throws Exception {
        mockMvc.perform(post("/auth/register")
                        .contentType(APPLICATION_JSON)
                        .content("""
                                {"email":"%s","password":"secret123"}
                                """.formatted(email)))
                .andExpect(status().isCreated());

        ArgumentCaptor<String> confirmationToken = ArgumentCaptor.forClass(String.class);
        verify(accountEmailService).sendEmailConfirmation(eq(email), confirmationToken.capture());
        return confirmationToken.getValue();
    }
}
