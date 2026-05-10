package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.infrastructure.config.AppProperties;
import br.com.stickerswap.shared.mail.MailProvider;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
public class AccountEmailServiceImpl implements AccountEmailService {

    private final MailProvider mailProvider;
    private final AppProperties appProperties;

    @Override
    public void sendEmailConfirmation(String email, String token) {
        String confirmationUrl = UriComponentsBuilder
                .fromUriString(appProperties.security().publicBaseUrl())
                .path("/auth/email-confirmations/confirm")
                .queryParam("token", token)
                .build()
                .toUriString();

        mailProvider.send(email, "Confirm your Sticker Swap email",
                "Confirm your email by opening this link:\n\n" + confirmationUrl + "\n\n"
                        + "This link expires soon. If you did not create a Sticker Swap account, ignore this email.");
    }

    @Override
    public void sendPasswordReset(String email, String token) {
        String resetUrl = UriComponentsBuilder
                .fromUriString(appProperties.security().passwordResetUrl())
                .queryParam("token", token)
                .build()
                .toUriString();

        mailProvider.send(email, "Reset your Sticker Swap password",
                "Reset your password using this link:\n\n" + resetUrl + "\n\n"
                        + "If the frontend is not available yet, use this token with the password reset API:\n\n"
                        + token + "\n\n"
                        + "If you did not request this, ignore this email.");
    }
}
