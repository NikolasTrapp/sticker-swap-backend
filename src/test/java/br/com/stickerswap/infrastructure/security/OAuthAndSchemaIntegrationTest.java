package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.support.PostgresIntegrationTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class OAuthAndSchemaIntegrationTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired JdbcTemplate jdbcTemplate;

    @Test
    void flywayAppliesOrganizedBaselineAndSeedsPkceClient() {
        List<String> versions = jdbcTemplate.queryForList(
                "SELECT version FROM flyway_schema_history ORDER BY installed_rank",
                String.class);
        assertThat(versions).containsExactly("1", "2", "3", "4", "5", "6", "7");

        Integer emailVerifiedColumnCount = jdbcTemplate.queryForObject(
                """
                SELECT COUNT(*)
                FROM information_schema.columns
                WHERE table_name = 'users'
                  AND column_name = 'email_verified'
                """,
                Integer.class);
        assertThat(emailVerifiedColumnCount).isEqualTo(1);

        String clientAuthenticationMethods = jdbcTemplate.queryForObject(
                "SELECT client_authentication_methods FROM oauth2_registered_client WHERE client_id = ?",
                String.class,
                "sticker-swap-web");
        assertThat(clientAuthenticationMethods).contains("none");

        String authorizationGrantTypes = jdbcTemplate.queryForObject(
                "SELECT authorization_grant_types FROM oauth2_registered_client WHERE client_id = ?",
                String.class,
                "sticker-swap-web");
        assertThat(authorizationGrantTypes).contains("authorization_code", "refresh_token");

        String scopes = jdbcTemplate.queryForObject(
                "SELECT scopes FROM oauth2_registered_client WHERE client_id = ?",
                String.class,
                "sticker-swap-web");
        assertThat(scopes).contains("openid", "profile", "api", "offline_access");

        String clientSettings = jdbcTemplate.queryForObject(
                "SELECT client_settings FROM oauth2_registered_client WHERE client_id = ?",
                String.class,
                "sticker-swap-web");
        assertThat(clientSettings).contains("require-proof-key");
    }

    @Test
    void oauthMetadataAndJwkSetAreExposedByAuthorizationServer() throws Exception {
        mockMvc.perform(get("/.well-known/oauth-authorization-server"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.issuer").value("http://localhost:8080"))
                .andExpect(jsonPath("$.authorization_endpoint").value("http://localhost:8080/oauth2/authorize"))
                .andExpect(jsonPath("$.token_endpoint").value("http://localhost:8080/oauth2/token"));

        mockMvc.perform(get("/oauth2/jwks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.keys").isArray());
    }
}
