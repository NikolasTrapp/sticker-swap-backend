package br.com.stickerswap.infrastructure.mail;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

class LogMailProviderTest {

    private final LogMailProvider provider = new LogMailProvider();

    @Test
    @DisplayName("dado email, assunto e corpo, quando enviar, então completa sem lançar exception")
    void givenEmailSubjectAndBody_whenSend_thenDoesNotThrow() {
        // Arrange / Act / Assert
        assertThatCode(() -> provider.send("to@example.com", "Subject", "Body text"))
                .doesNotThrowAnyException();
    }
}
