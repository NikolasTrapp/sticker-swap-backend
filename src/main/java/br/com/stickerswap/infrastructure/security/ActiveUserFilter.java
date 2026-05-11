package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import br.com.stickerswap.shared.error.ApiError;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.json.JsonMapper;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.oauth2.server.resource.authentication.JwtAuthenticationToken;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

import java.io.IOException;
import java.util.UUID;

@Component
@RequiredArgsConstructor
public class ActiveUserFilter extends OncePerRequestFilter {

    private static final String INACTIVE_ACCOUNT_MESSAGE = "Account is blocked or inactive";

    private final UserRepository userRepository;
    private final ObjectMapper objectMapper = JsonMapper.builder().findAndAddModules().build();

    @Override
    protected void doFilterInternal(
            HttpServletRequest request,
            HttpServletResponse response,
            FilterChain filterChain
    ) throws ServletException, IOException {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
        if (authentication instanceof JwtAuthenticationToken jwtAuthenticationToken
                && authentication.isAuthenticated()
                && protectedApiPath(request.getRequestURI())) {
            UUID userId = UUID.fromString(jwtAuthenticationToken.getToken().getSubject());
            boolean active = userRepository.existsByIdAndStatusAndEmailVerifiedTrue(userId, UserStatus.ACTIVE);
            if (!active) {
                SecurityContextHolder.clearContext();
                response.setStatus(HttpStatus.FORBIDDEN.value());
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                objectMapper.writeValue(response.getWriter(), ApiError.of(
                        HttpStatus.FORBIDDEN.value(),
                        "Forbidden",
                        INACTIVE_ACCOUNT_MESSAGE,
                        request.getRequestURI()
                ));
                return;
            }
        }

        filterChain.doFilter(request, response);
    }

    private boolean protectedApiPath(String path) {
        return !path.startsWith("/actuator/")
                && !path.startsWith("/v3/api-docs")
                && !path.startsWith("/swagger-ui")
                && !path.startsWith("/auth/")
                && !path.startsWith("/oauth2/")
                && !path.startsWith("/login")
                && !path.startsWith("/logout")
                && !path.startsWith("/ws");
    }
}
