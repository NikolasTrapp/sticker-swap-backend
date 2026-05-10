package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.config.AppProperties;
import com.nimbusds.jose.jwk.JWKSet;
import com.nimbusds.jose.jwk.RSAKey;
import com.nimbusds.jose.jwk.source.ImmutableJWKSet;
import com.nimbusds.jose.jwk.source.JWKSource;
import com.nimbusds.jose.proc.SecurityContext;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.annotation.web.configuration.OAuth2AuthorizationServerConfiguration;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.jwt.JwtDecoder;
import org.springframework.util.StringUtils;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.attribute.PosixFilePermission;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Set;
import java.util.UUID;

@Configuration
@RequiredArgsConstructor
@Slf4j
public class JwtConfig {

    private final AppProperties appProperties;

    @Bean
    public JWKSource<SecurityContext> jwkSource() {
        String jwkSetJson = appProperties.security().jwkSetJson();
        String jwkSetJsonFile = appProperties.security().jwkSetJsonFile();

        JWKSet jwkSet;
        if (StringUtils.hasText(jwkSetJson)) {
            jwkSet = parseConfiguredJwkSet(jwkSetJson);
        } else if (StringUtils.hasText(jwkSetJsonFile)) {
            jwkSet = loadOrCreatePersistedJwkSet(Path.of(jwkSetJsonFile));
        } else {
            jwkSet = generateEphemeralJwkSet();
        }
        return new ImmutableJWKSet<>(jwkSet);
    }

    @Bean
    public JwtDecoder jwtDecoder(JWKSource<SecurityContext> jwkSource) {
        return OAuth2AuthorizationServerConfiguration.jwtDecoder(jwkSource);
    }

    @Bean
    public PasswordEncoder passwordEncoder() {
        return new BCryptPasswordEncoder();
    }

    private JWKSet parseConfiguredJwkSet(String jwkSetJson) {
        try {
            return JWKSet.parse(jwkSetJson);
        } catch (Exception e) {
            throw new IllegalStateException("Failed to parse configured JWK set", e);
        }
    }

    private JWKSet loadOrCreatePersistedJwkSet(Path jwkSetPath) {
        Path absolutePath = jwkSetPath.toAbsolutePath();
        try {
            if (Files.exists(absolutePath) && Files.size(absolutePath) > 0) {
                log.info("Loading RSA JWK set from {}", absolutePath);
                return JWKSet.parse(Files.readString(absolutePath, StandardCharsets.UTF_8));
            }

            Path parent = absolutePath.getParent();
            if (parent != null) {
                Files.createDirectories(parent);
            }

            JWKSet generatedJwkSet = new JWKSet(generateRsaKey());
            Files.writeString(absolutePath, generatedJwkSet.toString(false), StandardCharsets.UTF_8);
            restrictOwnerOnly(absolutePath);
            log.warn("app.security.jwk-set-json is not configured. Generated and persisted a local RSA key at {}.",
                    absolutePath);
            return generatedJwkSet;
        } catch (Exception e) {
            throw new IllegalStateException("Failed to load or create JWK set file at " + absolutePath, e);
        }
    }

    private void restrictOwnerOnly(Path jwkSetPath) {
        try {
            Files.setPosixFilePermissions(jwkSetPath, Set.of(
                    PosixFilePermission.OWNER_READ,
                    PosixFilePermission.OWNER_WRITE
            ));
        } catch (Exception e) {
            log.debug("Could not tighten permissions for JWK set file {}", jwkSetPath, e);
        }
    }

    private JWKSet generateEphemeralJwkSet() {
        log.warn("app.security.jwk-set-json is not configured. Generating an ephemeral RSA key for local use.");
        return new JWKSet(generateRsaKey());
    }

    private RSAKey generateRsaKey() {
        try {
            KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA");
            generator.initialize(2048);
            KeyPair keyPair = generator.generateKeyPair();
            return new RSAKey.Builder((RSAPublicKey) keyPair.getPublic())
                    .privateKey((RSAPrivateKey) keyPair.getPrivate())
                    .keyID(UUID.randomUUID().toString())
                    .build();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to generate RSA key pair", e);
        }
    }
}
