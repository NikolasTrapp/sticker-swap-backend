package br.com.stickerswap.identity.application.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.web.util.UriComponentsBuilder;

@Service
@RequiredArgsConstructor
@Slf4j
public class AccountEmailServiceImpl implements AccountEmailService {

    private final ObjectProvider<JavaMailSender> mailSenderProvider;

    @Value("${app.mail.from:no-reply@stickerswap.com}")
    private String from;

    @Value("${app.mail.delivery-mode:smtp}")
    private String deliveryMode;

    @Value("${app.security.public-base-url:http://localhost:8080}")
    private String publicBaseUrl;

    @Value("${app.security.password-reset-url:http://localhost:8080/password-reset}")
    private String passwordResetUrl;

    @Override
    public void sendEmailConfirmation(String email, String token) {
        String confirmationUrl = UriComponentsBuilder.fromUriString(publicBaseUrl)
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
        String resetUrl = UriComponentsBuilder.fromUriString(passwordResetUrl)
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
        if ("log".equalsIgnoreCase(deliveryMode)) {
            log.debug("Mail delivery is in log mode. Email to {} with subject '{}':\n{}", to, subject, text);
            return;
        }

        JavaMailSender mailSender = mailSenderProvider.getIfAvailable();
        if (mailSender == null) {
            log.debug("Mail sender is not configured. Email to {} with subject '{}':\n{}", to, subject, text);
            return;
        }

        SimpleMailMessage message = new SimpleMailMessage();
        message.setFrom(from);
        message.setTo(to);
        message.setSubject(subject);
        message.setText(text);
        mailSender.send(message);
    }
}
