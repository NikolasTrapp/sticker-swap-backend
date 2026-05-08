package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.shared.error.RateLimitExceededException;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.Instant;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Service
public class InMemoryRateLimiterService implements RateLimiterService {

    private final Map<String, Window> windows = new ConcurrentHashMap<>();

    @Override
    public void consume(String key, int maxRequests, Duration window) {
        Instant now = Instant.now();
        Window state = windows.computeIfAbsent(key, ignored -> new Window(now, 0));

        synchronized (state) {
            if (!state.startedAt.plus(window).isAfter(now)) {
                state.startedAt = now;
                state.count = 0;
            }
            if (state.count >= maxRequests) {
                throw new RateLimitExceededException("Rate limit exceeded");
            }
            state.count++;
        }
    }

    private static final class Window {
        private Instant startedAt;
        private int count;

        private Window(Instant startedAt, int count) {
            this.startedAt = startedAt;
            this.count = count;
        }
    }
}
