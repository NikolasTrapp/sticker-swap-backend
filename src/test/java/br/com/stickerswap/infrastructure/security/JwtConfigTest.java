package br.com.stickerswap.infrastructure.security;

import br.com.stickerswap.infrastructure.config.AppProperties;
import com.nimbusds.jose.jwk.JWKSet;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.file.Files;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;

class JwtConfigTest {

    @TempDir
    Path tempDir;

    @Test
    void jwkSourcePersistsGeneratedJwkSetForRestartReuse() throws Exception {
        Path jwkSetFile = tempDir.resolve("jwk-set.json");
        JwtConfig jwtConfig = new JwtConfig(props("", jwkSetFile.toString()));

        jwtConfig.jwkSource();

        assertThat(jwkSetFile).exists();
        JWKSet firstJwkSet = JWKSet.parse(Files.readString(jwkSetFile));
        assertThat(firstJwkSet.containsNonPublicKeys()).isTrue();
        String firstKeyId = firstJwkSet.getKeys().get(0).getKeyID();

        jwtConfig.jwkSource();

        JWKSet secondJwkSet = JWKSet.parse(Files.readString(jwkSetFile));
        assertThat(secondJwkSet.getKeys()).hasSize(1);
        assertThat(secondJwkSet.getKeys().get(0).getKeyID()).isEqualTo(firstKeyId);
    }

    private static AppProperties props(String jwkSetJson, String jwkSetJsonFile) {
        var security = new AppProperties.SecurityProperties(
                null, null, null, null,
                jwkSetJson,
                jwkSetJsonFile,
                null
        );
        return new AppProperties(security, null, null, null);
    }
}
