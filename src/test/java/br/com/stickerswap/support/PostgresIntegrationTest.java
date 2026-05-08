package br.com.stickerswap.support;

import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

@SuppressWarnings("resource")
@Testcontainers
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    @Container
    @ServiceConnection
    protected static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                    .withReuse(true)
                    .withDatabaseName("stickerswap_test")
                    .withUsername("stickerswap")
                    .withPassword("stickerswap")
                    .withStartupTimeout(Duration.ofSeconds(60))
                    .withLabel("com.stickerswap.testcontainer", "postgres");
}
