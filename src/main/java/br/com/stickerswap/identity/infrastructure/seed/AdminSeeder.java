package br.com.stickerswap.identity.infrastructure.seed;

import br.com.stickerswap.identity.domain.model.User;
import br.com.stickerswap.identity.domain.model.UserRole;
import br.com.stickerswap.identity.domain.model.UserStatus;
import br.com.stickerswap.identity.infrastructure.persistence.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
@Slf4j
public class AdminSeeder implements ApplicationRunner {

    @Value("${app.admin.email:admin@stickerswap.com}")
    private String adminEmail;

    @Value("${app.admin.password:changeme}")
    private String adminPassword;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(ApplicationArguments args) {
        if (userRepository.findByEmail(adminEmail).isEmpty()) {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setPasswordHash(passwordEncoder.encode(adminPassword));
            admin.setRole(UserRole.ADMIN);
            admin.setStatus(UserStatus.ACTIVE);
            admin.setEmailVerified(true);
            admin.setEmailVerifiedAt(java.time.Instant.now());
            userRepository.save(admin);
            log.info("Admin user created: {}", adminEmail);
        }
    }
}
