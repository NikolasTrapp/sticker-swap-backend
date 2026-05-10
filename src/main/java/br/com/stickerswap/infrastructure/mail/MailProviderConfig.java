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
        if ("log".equalsIgnoreCase(mode)) {
            log.info("Mail provider: log");
            return new LogMailProvider();
        }
        log.info("Mail provider: resend");
        return new ResendMailProvider(props);
    }
}
