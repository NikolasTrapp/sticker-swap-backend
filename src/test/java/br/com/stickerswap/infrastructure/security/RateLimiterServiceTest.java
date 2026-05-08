package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.shared.error.RateLimitExceededException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RateLimiterServiceTest {

    private final RateLimiterService rateLimiterService = new InMemoryRateLimiterService();

    @Test
    void consume_throwsWhenWindowLimitIsExceeded() {
        rateLimiterService.consume("key", 2, Duration.ofMinutes(1));
        rateLimiterService.consume("key", 2, Duration.ofMinutes(1));

        assertThatThrownBy(() -> rateLimiterService.consume("key", 2, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceededException.class);
    }

    @Test
    void consume_tracksIndependentKeys() {
        rateLimiterService.consume("a", 1, Duration.ofMinutes(1));
        rateLimiterService.consume("b", 1, Duration.ofMinutes(1));

        assertThatThrownBy(() -> rateLimiterService.consume("a", 1, Duration.ofMinutes(1)))
                .isInstanceOf(RateLimitExceededException.class);
    }
}
