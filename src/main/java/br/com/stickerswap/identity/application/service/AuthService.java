package br.com.stickerswap.identity.application.service;

import br.com.stickerswap.identity.application.dto.RegisterRequest;
import br.com.stickerswap.identity.domain.model.User;

public interface AuthService {

    User register(RegisterRequest request);

    void resendEmailConfirmation(String email);

    User confirmEmail(String rawToken);

    void requestPasswordReset(String email);

    void resetPassword(String rawToken, String newPassword);
}
