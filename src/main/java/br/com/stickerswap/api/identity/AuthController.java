package br.com.stickerswap.api.identity;

import br.com.stickerswap.api.identity.dto.EmailRequest;
import br.com.stickerswap.api.identity.dto.PasswordResetRequest;
import br.com.stickerswap.api.identity.dto.RegisterRequest;
import br.com.stickerswap.api.identity.dto.UserResponse;
import br.com.stickerswap.domain.identity.service.AuthService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/auth")
@Tag(name = "Auth", description = "Registration and login")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;

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

    @GetMapping("/email-confirmations/confirm")
    @Operation(summary = "Confirm a user email address")
    public UserResponse confirmEmail(@RequestParam String token) {
        return UserResponse.from(authService.confirmEmail(token));
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
