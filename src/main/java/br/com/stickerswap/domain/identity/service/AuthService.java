package br.com.stickerswap.domain.identity.service;

import br.com.stickerswap.api.identity.dto.RegisterRequest;
import br.com.stickerswap.domain.identity.model.User;

public interface AuthService {

    User register(RegisterRequest request);

    void resendEmailConfirmation(String email);

    User confirmEmail(String rawToken);

    void requestPasswordReset(String email);

    void resetPassword(String rawToken, String newPassword);
}
