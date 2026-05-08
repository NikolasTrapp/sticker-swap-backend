package br.com.stickerswap.infrastructure.security;

import java.time.Duration;

public interface RateLimiterService {

    void consume(String key, int maxRequests, Duration window);
}
