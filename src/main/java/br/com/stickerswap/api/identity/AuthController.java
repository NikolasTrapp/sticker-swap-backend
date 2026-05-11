package br.com.stickerswap.api.identity;

import br.com.stickerswap.api.identity.dto.EmailRequest;
import br.com.stickerswap.api.identity.dto.PasswordResetRequest;
import br.com.stickerswap.api.identity.dto.RegisterRequest;
import br.com.stickerswap.api.identity.dto.UserResponse;
import br.com.stickerswap.domain.identity.service.AuthService;
import br.com.stickerswap.infrastructure.config.AppProperties;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.UriComponentsBuilder;

import java.net.URI;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Registration and login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final AppProperties appProperties;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.CREATED)
    @Operation(summary = "Register a new user account")
    public UserResponse register(@Valid @RequestBody RegisterRequest request) {
        return UserResponse.from(authService.register(request));
    }

    @PostMapping("/email-confirmations")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Resend an email confirmation link")
    public void resendEmailConfirmation(@Valid @RequestBody EmailRequest request) {
        authService.resendEmailConfirmation(request.email());
    }

    @GetMapping(value = "/email-confirmations/confirm", params = "redirect=false", produces = MediaType.APPLICATION_JSON_VALUE)
    @Operation(summary = "Confirm a user email address")
    public UserResponse confirmEmailApi(@RequestParam String token) {
        return UserResponse.from(authService.confirmEmail(token));
    }

    @GetMapping("/email-confirmations/confirm")
    @Operation(summary = "Confirm a user email address and redirect to confirmation success page")
    public ResponseEntity<Void> confirmEmail(@RequestParam String token) {
        authService.confirmEmail(token);
        return ResponseEntity.status(HttpStatus.FOUND).location(frontendEmailConfirmedUrl()).build();
    }

    private URI frontendEmailConfirmedUrl() {
        URI loginUrl = URI.create(appProperties.security().frontendLoginUrl());
        String path = loginUrl.getPath() == null ? "" : loginUrl.getPath();
        String normalizedPath = path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
        String basePath = normalizedPath.endsWith("/login")
                ? normalizedPath.substring(0, normalizedPath.length() - "/login".length())
                : normalizedPath;

        if (basePath.endsWith("/")) {
            basePath = basePath.substring(0, basePath.length() - 1);
        }

        String targetPath = basePath + "/email-confirmed";
        return UriComponentsBuilder
                .fromUri(loginUrl)
                .replacePath(targetPath)
                .replaceQuery(null)
                .fragment(null)
                .build()
                .toUri();
    }

    @PostMapping("/password-reset-requests")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Request a password reset email")
    public void requestPasswordReset(@Valid @RequestBody EmailRequest request) {
        authService.requestPasswordReset(request.email());
    }

    @PostMapping("/password-resets")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    @Operation(summary = "Reset password with a valid password reset token")
    public void resetPassword(@Valid @RequestBody PasswordResetRequest request) {
        authService.resetPassword(request.token(), request.newPassword());
    }
}
