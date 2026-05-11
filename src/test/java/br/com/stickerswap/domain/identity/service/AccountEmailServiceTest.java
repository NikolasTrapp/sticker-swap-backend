package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.infrastructure.config.AppProperties;
import br.com.stickerswap.shared.mail.MailProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class AccountEmailServiceTest {

    @Mock MailProvider mailProvider;
    @Mock AppProperties appProperties;
    @Mock AppProperties.SecurityProperties securityProperties;

    @InjectMocks AccountEmailServiceImpl accountEmailService;

    @Test
    @DisplayName("dado email e token, quando enviar confirmação, então provider recebe URL de confirmação correta")
    void givenEmailAndToken_whenSendEmailConfirmation_thenMailProviderReceivesCorrectUrl() {
        // Arrange
        when(appProperties.security()).thenReturn(securityProperties);
        when(securityProperties.publicBaseUrl()).thenReturn("http://api.example.com");
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        accountEmailService.sendEmailConfirmation("user@example.com", "my-token");

        // Assert
        verify(mailProvider).send(eq("user@example.com"), any(), bodyCaptor.capture());
        assertThat(bodyCaptor.getValue())
                .contains("http://api.example.com/auth/email-confirmations/confirm?token=my-token");
    }

    @Test
    @DisplayName("dado email e token, quando enviar reset de senha, então provider recebe URL de reset correta com token embutido")
    void givenEmailAndToken_whenSendPasswordReset_thenMailProviderReceivesPasswordResetUrlWithToken() {
        // Arrange
        when(appProperties.security()).thenReturn(securityProperties);
        when(securityProperties.passwordResetUrl()).thenReturn("http://localhost:4200/password-reset-request");
        ArgumentCaptor<String> bodyCaptor = ArgumentCaptor.forClass(String.class);

        // Act
        accountEmailService.sendPasswordReset("user@example.com", "reset-token");

        // Assert
        verify(mailProvider).send(eq("user@example.com"), any(), bodyCaptor.capture());
        String body = bodyCaptor.getValue();
        assertThat(body).contains("http://localhost:4200/password-reset-request?token=reset-token");
        assertThat(body).contains("reset-token");
    }
}
