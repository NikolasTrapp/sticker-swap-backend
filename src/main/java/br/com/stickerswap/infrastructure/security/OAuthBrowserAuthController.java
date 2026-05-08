package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.web.context.HttpSessionSecurityContextRepository;
import org.springframework.security.web.csrf.CsrfToken;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.time.Instant;

@RestController
@RequiredArgsConstructor
public class OAuthBrowserAuthController {

    private final AuthenticationManager authenticationManager;
    private final UserRepository userRepository;

    @GetMapping("/oauth2/csrf")
    public CsrfToken csrf(CsrfToken csrfToken) {
        return csrfToken;
    }

    @PostMapping("/oauth2/login")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Transactional
    public void login(@Valid @RequestBody OAuthLoginRequest request, HttpServletRequest servletRequest) {
        String email = request.email().trim().toLowerCase();
        var authentication = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(email, request.password()));

        var context = SecurityContextHolder.createEmptyContext();
        context.setAuthentication(authentication);
        SecurityContextHolder.setContext(context);

        var session = servletRequest.getSession(true);
        servletRequest.changeSessionId();
        session.setAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY, context);

        userRepository.findByEmail(email)
                .ifPresent(user -> {
                    user.setLastActivityAt(Instant.now());
                    userRepository.save(user);
                });
    }

    public record OAuthLoginRequest(
            @Email @NotBlank String email,
            @NotBlank String password
    ) {}
}
