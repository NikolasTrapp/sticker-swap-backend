package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.shared.error.ApiError;
import br.com.stickerswap.shared.error.RateLimitExceededException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
public class RateLimitingFilter extends OncePerRequestFilter {

    private final RateLimiterService rateLimiterService;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    public RateLimitingFilter(RateLimiterService rateLimiterService) {
        this.rateLimiterService = rateLimiterService;
    }

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        try {
            applyLimits(request);
            filterChain.doFilter(request, response);
        } catch (RateLimitExceededException ex) {
            log.warn("Rate limit exceeded: path={} ip={}", request.getRequestURI(), clientIp(request));
            response.setStatus(429);
            response.setContentType(MediaType.APPLICATION_JSON_VALUE);
            objectMapper.writeValue(response.getWriter(),
                    ApiError.of(429, "Too Many Requests", ex.getMessage(), request.getRequestURI()));
        }
    }

    private void applyLimits(HttpServletRequest request) {
        String path = request.getRequestURI();
        String method = request.getMethod();
        String ip = clientIp(request);

        if ("POST".equals(method) && "/auth/register".equals(path)) {
            rateLimiterService.consume("ip:register:" + ip, 5, Duration.ofHours(1));
            return;
        }

        if ("POST".equals(method) && "/auth/email-confirmations".equals(path)) {
            rateLimiterService.consume("ip:email-confirmation:" + ip, 10, Duration.ofHours(1));
            return;
        }

        if ("POST".equals(method) && "/auth/password-reset-requests".equals(path)) {
            rateLimiterService.consume("ip:password-reset-request:" + ip, 10, Duration.ofHours(1));
            return;
        }

        if ("POST".equals(method) && ("/login".equals(path) || "/oauth2/login".equals(path))) {
            rateLimiterService.consume("ip:login:" + ip, 10, Duration.ofMinutes(1));
            return;
        }

        if ("POST".equals(method) && "/oauth2/token".equals(path)) {
            String clientId = request.getParameter("client_id");
            rateLimiterService.consume("ip:oauth-token:" + ip + ":" + nullToAnonymous(clientId),
                    30, Duration.ofMinutes(1));
            return;
        }

        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken
                && authentication.isAuthenticated()
                && protectedApiPath(path)) {
            rateLimiterService.consume("user:api:" + jwtAuthenticationToken.getToken().getSubject(),
                    300, Duration.ofMinutes(1));
        }
    }

    private boolean protectedApiPath(String path) {
        return !path.startsWith("/actuator/")
                && !path.startsWith("/v3/api-docs")
                && !path.startsWith("/swagger-ui")
                && !path.startsWith("/auth/")
                && !path.startsWith("/oauth2/")
                && !path.startsWith("/login")
                && !path.startsWith("/ws");
    }

    private String clientIp(HttpServletRequest request) {
        String forwardedFor = request.getHeader("X-Forwarded-For");
        if (forwardedFor != null && !forwardedFor.isBlank()) {
            return forwardedFor.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private String nullToAnonymous(String value) {
        return value == null || value.isBlank() ? "anonymous" : value;
    }
}
