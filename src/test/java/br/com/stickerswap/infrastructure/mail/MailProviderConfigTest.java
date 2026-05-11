package br.com.stickerswap.infrastructure.mail;

import br.com.stickerswap.infrastructure.config.AppProperties;
import br.com.stickerswap.shared.mail.MailProvider;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class MailProviderConfigTest {

    @Mock AppProperties appProperties;
    @Mock AppProperties.MailProperties mailProperties;

    private final MailProviderConfig config = new MailProviderConfig();

    @Test
    @DisplayName("dado modo 'log', quando construir bean, então retorna LogMailProvider")
    void givenLogMode_whenBuildingBean_thenReturnsLogMailProvider() {
        // Arrange
        when(appProperties.mail()).thenReturn(mailProperties);
        when(mailProperties.deliveryMode()).thenReturn("log");

        // Act
        MailProvider provider = config.mailProvider(appProperties);

        // Assert
        assertThat(provider).isInstanceOf(LogMailProvider.class);
    }

    @Test
    @DisplayName("dado modo 'brevo', quando construir bean, então retorna BrevoMailProvider")
    void givenBrevoMode_whenBuildingBean_thenReturnsBrevoMailProvider() {
        // Arrange
        when(appProperties.mail()).thenReturn(mailProperties);
        when(mailProperties.deliveryMode()).thenReturn("brevo");

        // Act
        MailProvider provider = config.mailProvider(appProperties);

        // Assert
        assertThat(provider).isInstanceOf(BrevoMailProvider.class);
    }

    @Test
    @DisplayName("dado modo 'resend', quando construir bean, então retorna ResendMailProvider")
    void givenResendMode_whenBuildingBean_thenReturnsResendMailProvider() {
        // Arrange
        when(appProperties.mail()).thenReturn(mailProperties);
        when(mailProperties.deliveryMode()).thenReturn("resend");

        // Act
        MailProvider provider = config.mailProvider(appProperties);

        // Assert
        assertThat(provider).isInstanceOf(ResendMailProvider.class);
    }

    @Test
    @DisplayName("dado modo nulo, quando construir bean, então padrão é LogMailProvider")
    void givenNullMode_whenBuildingBean_thenDefaultsToLogMailProvider() {
        // Arrange
        when(appProperties.mail()).thenReturn(mailProperties);
        when(mailProperties.deliveryMode()).thenReturn(null);

        // Act
        MailProvider provider = config.mailProvider(appProperties);

        // Assert
        assertThat(provider).isInstanceOf(LogMailProvider.class);
    }

    @Test
    @DisplayName("dado modo desconhecido, quando construir bean, então padrão é LogMailProvider")
    void givenUnknownMode_whenBuildingBean_thenDefaultsToLogMailProvider() {
        // Arrange
        when(appProperties.mail()).thenReturn(mailProperties);
        when(mailProperties.deliveryMode()).thenReturn("smtp");

        // Act
        MailProvider provider = config.mailProvider(appProperties);

        // Assert
        assertThat(provider).isInstanceOf(LogMailProvider.class);
    }
}
