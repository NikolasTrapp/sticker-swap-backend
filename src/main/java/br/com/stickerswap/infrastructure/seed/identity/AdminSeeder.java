package br.com.stickerswap.infrastructure.seed.identity;

import br.com.stickerswap.domain.identity.model.User;
import br.com.stickerswap.domain.identity.model.UserRole;
import br.com.stickerswap.domain.identity.model.UserStatus;
import br.com.stickerswap.infrastructure.config.AppProperties;
import br.com.stickerswap.infrastructure.repository.identity.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    private final AppProperties appProperties;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        String email = appProperties.admin().email();
        if (userRepository.findByEmail(email).isEmpty()) {
            User admin = new User();
            admin.setEmail(email);
            admin.setPasswordHash(passwordEncoder.encode(appProperties.admin().password()));
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmailVerified(true);
            admin.setEmailVerifiedAt(LocalDateTime.now());
            userRepository.save(admin);
            log.info("Admin user created: {}", email);
        }
    }
}
