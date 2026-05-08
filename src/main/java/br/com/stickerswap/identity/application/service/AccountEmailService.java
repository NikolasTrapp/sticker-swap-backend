package br.com.stickerswap.identity.application.service;

public interface AccountEmailService {

    void sendEmailConfirmation(String email, String token);

    void sendPasswordReset(String email, String token);
}
