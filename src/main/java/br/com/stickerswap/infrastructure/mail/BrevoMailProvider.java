package br.com.stickerswap.infrastructure.mail;

import br.com.stickerswap.infrastructure.config.AppProperties;
import br.com.stickerswap.shared.mail.MailProvider;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
class BrevoMailProvider implements MailProvider {

    private record Sender(String email) {}
    private record Recipient(String email) {}
    private record BrevoRequest(
            Sender sender,
            List<Recipient> to,
            String subject,
            String textContent
    ) {}
    private record BrevoResponse(String messageId) {}

    private final RestClient restClient;
    private final String from;

    BrevoMailProvider(AppProperties props) {
        AppProperties.MailProperties mail = props.mail();
        this.from = mail.from();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.brevo.com")
                .defaultHeader("api-key", mail.apiKey())
                .requestFactory(factory)
                .build();
    }

    @Override
    public void send(String to, String subject, String text) {
        BrevoResponse response = restClient.post()
                .uri("/v3/smtp/email")
                .body(new BrevoRequest(new Sender(from), List.of(new Recipient(to)), subject, text))
                .retrieve()
                .body(BrevoResponse.class);

        log.debug("Brevo email queued messageId={} to={} subject='{}'",
                response != null ? response.messageId() : "?", to, subject);
    }
}
