package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.infrastructure.config.AppProperties;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEmailServiceImpl implements AccountEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;
    private final AppProperties appProperties;

    @Override
    public void sendEmailConfirmation(String email, String token) {
        String confirmationUrl = UriComponentsBuilder
                .fromUriString(appProperties.security().publicBaseUrl())
                .path("/auth/email-confirmations/confirm")
                .queryParam("token", token)
                .build()
                .toUriString();

        send(email, "Confirm your Sticker Swap email",
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

        send(email, "Reset your Sticker Swap password",
                "Reset your password using this link:\n\n" + resetUrl + "\n\n"
                        + "If the frontend is not available yet, use this token with the password reset API:\n\n"
                        + token + "\n\n"
                        + "If you did not request this, ignore this email.");
    }

    private void send(String to, String subject, String text) {
        if ("log".equalsIgnoreCase(appProperties.mail().deliveryMode())) {
            log.info("Mail delivery is in log mode. Email to {} with subject '{}':\n{}", to, subject, text);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.warn("Mail sender is not configured. Email to {} with subject '{}':\n{}", to, subject, text);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(appProperties.mail().from());
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
