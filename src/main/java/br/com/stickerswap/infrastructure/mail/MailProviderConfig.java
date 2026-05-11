package br.com.stickerswap.infrastructure.mail;

import br.com.stickerswap.infrastructure.config.AppProperties;
import br.com.stickerswap.shared.mail.MailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@Slf4j
public class MailProviderConfig {

    @Bean
    public MailProvider mailProvider(AppProperties props) {
        String mode = props.mail().deliveryMode();
        return switch (mode != null ? mode.toLowerCase() : "log") {
            case "brevo" -> {
                log.info("Mail provider: brevo");
                yield new BrevoMailProvider(props);
            }
            case "resend" -> {
                log.info("Mail provider: resend");
                yield new ResendMailProvider(props);
            }
            default -> {
                log.info("Mail provider: log");
                yield new LogMailProvider();
            }
        };
    }
}
