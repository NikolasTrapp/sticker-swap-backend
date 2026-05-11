package br.com.stickerswap.domain.profile.service;

import br.com.stickerswap.infrastructure.config.AppProperties;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;

class CepGeocodeServiceTest {

    private HttpServer server;

    @AfterEach
    void tearDown() {
        if (server != null) {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("dado CEP com caracteres não numéricos e tamanho inválido, quando resolver, então retorna vazio sem chamar API")
    void givenInvalidCepLength_whenResolve_thenReturnsEmptyWithoutCallingApi() {
        // Arrange
        CepGeocodeService service = new CepGeocodeService(props("http://127.0.0.1:1"));

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("12.345");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("dado CEP encontrado com coordenadas, quando resolver, então retorna cidade, estado e coordenadas")
    void givenFoundCepWithCoordinates_whenResolve_thenReturnsLocationWithCoordinates() throws IOException {
        // Arrange
        CepGeocodeService service = serviceResponding(200, """
                {
                  "city": "Blumenau",
                  "state": "SC",
                  "location": {
                    "coordinates": {
                      "latitude": -26.9187527,
                      "longitude": -49.0660250
                    }
                  }
                }
                """);

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("89037-504");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().city()).isEqualTo("Blumenau");
        assertThat(result.get().state()).isEqualTo("SC");
        assertThat(result.get().latitude()).isEqualByComparingTo(new BigDecimal("-26.9187527"));
        assertThat(result.get().longitude()).isEqualByComparingTo(new BigDecimal("-49.0660250"));
    }

    @Test
    @DisplayName("dado CEP encontrado sem bloco de localização, quando resolver, então retorna cidade e estado sem coordenadas")
    void givenFoundCepWithoutLocation_whenResolve_thenReturnsLocationWithoutCoordinates() throws IOException {
        // Arrange
        CepGeocodeService service = serviceResponding(200, """
                {
                  "city": "Porto Alegre",
                  "state": "RS",
                  "location": null
                }
                """);

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("90040060");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().city()).isEqualTo("Porto Alegre");
        assertThat(result.get().state()).isEqualTo("RS");
        assertThat(result.get().latitude()).isNull();
        assertThat(result.get().longitude()).isNull();
    }

    @Test
    @DisplayName("dado CEP encontrado com localização sem coordenadas, quando resolver, então retorna sem latitude e longitude")
    void givenFoundCepWithoutCoordinates_whenResolve_thenReturnsLocationWithNullCoordinates() throws IOException {
        // Arrange
        CepGeocodeService service = serviceResponding(200, """
                {
                  "city": "Curitiba",
                  "state": "PR",
                  "location": {}
                }
                """);

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("80010000");

        // Assert
        assertThat(result).isPresent();
        assertThat(result.get().city()).isEqualTo("Curitiba");
        assertThat(result.get().state()).isEqualTo("PR");
        assertThat(result.get().latitude()).isNull();
        assertThat(result.get().longitude()).isNull();
    }

    @Test
    @DisplayName("dada resposta sem corpo, quando resolver CEP, então retorna vazio")
    void givenEmptyBody_whenResolve_thenReturnsEmpty() throws IOException {
        // Arrange
        CepGeocodeService service = serviceResponding(204, "");

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("01001000");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("dada resposta sem cidade, quando resolver CEP, então retorna vazio")
    void givenResponseWithoutCity_whenResolve_thenReturnsEmpty() throws IOException {
        // Arrange
        CepGeocodeService service = serviceResponding(200, """
                {
                  "city": "",
                  "state": "SP"
                }
                """);

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("01001000");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("dada resposta sem estado, quando resolver CEP, então retorna vazio")
    void givenResponseWithoutState_whenResolve_thenReturnsEmpty() throws IOException {
        // Arrange
        CepGeocodeService service = serviceResponding(200, """
                {
                  "city": "São Paulo",
                  "state": " "
                }
                """);

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("01001000");

        // Assert
        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("dada falha da API de CEP, quando resolver, então retorna vazio")
    void givenCepApiFailure_whenResolve_thenReturnsEmpty() throws IOException {
        // Arrange
        CepGeocodeService service = serviceResponding(500, """
                {
                  "message": "unexpected error"
                }
                """);

        // Act
        Optional<CepGeocodeService.CepLocation> result = service.resolve("01001000");

        // Assert
        assertThat(result).isEmpty();
    }

    private CepGeocodeService serviceResponding(int status, String body) throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext("/api/cep/v2/89037504", exchange -> respond(exchange, status, body));
        server.createContext("/api/cep/v2/90040060", exchange -> respond(exchange, status, body));
        server.createContext("/api/cep/v2/80010000", exchange -> respond(exchange, status, body));
        server.createContext("/api/cep/v2/01001000", exchange -> respond(exchange, status, body));
        server.start();
        return new CepGeocodeService(props("http://127.0.0.1:" + server.getAddress().getPort()));
    }

    private static void respond(HttpExchange exchange, int status, String body) throws IOException {
        byte[] bytes = body.getBytes(StandardCharsets.UTF_8);
        exchange.getResponseHeaders().add("Content-Type", "application/json");
        exchange.sendResponseHeaders(status, bytes.length);
        exchange.getResponseBody().write(bytes);
        exchange.close();
    }

    private static AppProperties props(String apiBaseUrl) {
        AppProperties.CepProperties cep = new AppProperties.CepProperties(
                apiBaseUrl,
                Duration.ofSeconds(1),
                Duration.ofSeconds(1)
        );
        return new AppProperties(null, null, null, null, null, cep);
    }
}
