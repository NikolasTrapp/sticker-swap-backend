package br.com.stickerswap.infrastructure.mail;

import br.com.stickerswap.infrastructure.config.AppProperties;
import br.com.stickerswap.shared.mail.MailProvider;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestClient;

import java.util.List;

@Slf4j
class ResendMailProvider implements MailProvider {

    private record ResendRequest(
            String from,
            List<String> to,
            String subject,
            String text
    ) {}

    private record ResendResponse(@JsonProperty("id") String id) {}

    private final RestClient restClient;
    private final String from;

    ResendMailProvider(AppProperties props) {
        AppProperties.MailProperties mail = props.mail();
        this.from = mail.from();

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(5_000);
        factory.setReadTimeout(10_000);

        this.restClient = RestClient.builder()
                .baseUrl("https://api.resend.com")
                .defaultHeader("Authorization", "Bearer " + mail.apiKey())
                .requestFactory(factory)
                .build();
    }

    @Override
    public void send(String to, String subject, String text) {
        ResendResponse response = restClient.post()
                .uri("/emails")
                .body(new ResendRequest(from, List.of(to), subject, text))
                .retrieve()
                .body(ResendResponse.class);

        log.debug("Resend email queued id={} to={} subject='{}'",
                response != null ? response.id() : "?", to, subject);
    }
}
