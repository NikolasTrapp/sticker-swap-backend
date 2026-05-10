package br.com.stickerswap.domain.profile.service;

import br.com.stickerswap.infrastructure.config.AppProperties;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.math.BigDecimal;
import java.util.Optional;

@Service
@Slf4j
public class CepGeocodeService {

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CepApiResponse(String city, String state, LocationWrapper location) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record LocationWrapper(CoordinatesWrapper coordinates) {}

    @JsonIgnoreProperties(ignoreUnknown = true)
    record CoordinatesWrapper(BigDecimal latitude, BigDecimal longitude) {}

    public record CepLocation(String city, String state,
                               BigDecimal latitude, BigDecimal longitude) {}

    private final RestClient restClient;

    public CepGeocodeService(AppProperties props) {
        AppProperties.CepProperties cep = props.cep();
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(cep.connectTimeout());
        factory.setReadTimeout(cep.readTimeout());
        this.restClient = RestClient.builder()
                .baseUrl(cep.apiBaseUrl())
                .requestFactory(factory)
                .build();
    }

    public Optional<CepLocation> resolve(String rawCep) {
        String digits = rawCep.replaceAll("\\D", "");
        try {
            CepApiResponse resp = restClient.get()
                    .uri("/api/cep/v2/{cep}", digits)
                    .retrieve()
                    .body(CepApiResponse.class);

            if (resp == null
                    || resp.location() == null
                    || resp.location().coordinates() == null
                    || resp.location().coordinates().latitude() == null
                    || resp.location().coordinates().longitude() == null) {
                return Optional.empty();
            }

            return Optional.of(new CepLocation(
                    resp.city(),
                    resp.state(),
                    resp.location().coordinates().latitude(),
                    resp.location().coordinates().longitude()));
        } catch (Exception e) {
            log.warn("CEP geocoding failed for '{}': {}", digits, e.getMessage());
            return Optional.empty();
        }
    }
}
