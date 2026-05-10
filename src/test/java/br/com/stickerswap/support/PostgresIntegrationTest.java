package br.com.stickerswap.support;

import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.utility.DockerImageName;

import java.time.Duration;

/**
 * Base class for integration tests that require a real PostgreSQL instance.
 *
 * The container is started ONCE per JVM via the static block, so all test
 * classes that extend this share the same running instance.  Spring contexts
 * with different configurations (e.g. with/without MockMvc) can each have
 * their own HikariPool pointing to the same DB — Flyway will only migrate
 * the schema on the first context that starts.
 *
 * @DynamicPropertySource is called for every new Spring test context, always
 * returning the same stable JDBC URL, which is what allows the Spring context
 * cache to reuse contexts where possible and, when it can't, still connect to
 * the live container.
 */
@ActiveProfiles("test")
public abstract class PostgresIntegrationTest {

    @SuppressWarnings("resource")
    private static final PostgreSQLContainer<?> POSTGRES;

    static {
        POSTGRES = new PostgreSQLContainer<>(DockerImageName.parse("postgres:16-alpine"))
                .withDatabaseName("stickerswap_test")
                .withUsername("stickerswap")
                .withPassword("stickerswap")
                .withStartupTimeout(Duration.ofSeconds(60));
        POSTGRES.start();
    }

    @DynamicPropertySource
    static void registerContainerProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }
}
