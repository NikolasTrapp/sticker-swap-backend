package br.com.stickerswap.domain.identity.service;

public interface AccountEmailService {

    void sendEmailConfirmation(String email, String token);

    void sendPasswordReset(String email, String token);
}
