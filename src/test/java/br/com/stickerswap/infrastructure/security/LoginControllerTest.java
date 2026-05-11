package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.config.AppProperties;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;
import br.com.stickerswap.support.PostgresIntegrationTest;

@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
class LoginControllerTest extends PostgresIntegrationTest {

    @Autowired MockMvc mockMvc;
    @Autowired AppProperties appProperties;

    @Test
    @DisplayName("dado GET /login, quando página de login é solicitada, então retorna view 'login' com URLs do frontend")
    void givenLoginPageRequest_whenGet_thenReturnsLoginViewWithFrontendUrls() throws Exception {
        // Arrange
        String expectedLoginUrl = appProperties.security().frontendLoginUrl();
        String expectedPasswordResetUrl = expectedLoginUrl.replace("/login", "/password-reset-request");
        String expectedRegisterUrl = expectedLoginUrl.replace("/login", "/register");

        // Act / Assert
        mockMvc.perform(get("/login"))
                .andExpect(status().isOk())
                .andExpect(view().name("login"))
                .andExpect(model().attribute("passwordResetUrl", expectedPasswordResetUrl))
                .andExpect(model().attribute("registerUrl", expectedRegisterUrl));
    }
}
