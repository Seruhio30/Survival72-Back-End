package com.seruhioCode30.survival72.service.join;

import org.junit.jupiter.api.Test;

import java.util.Base64;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class ManagementTokenServiceTests {

    private final ManagementTokenService managementTokenService = new ManagementTokenService();

    @Test
    void generatesUrlSafeTokenFromAtLeast32RandomBytes() {
        String token = managementTokenService.generateToken();

        byte[] decoded = Base64.getUrlDecoder().decode(token);

        assertThat(decoded).hasSizeGreaterThanOrEqualTo(32);
        assertThat(token).doesNotContain("=");
    }

    @Test
    void generatesDifferentTokens() {
        String firstToken = managementTokenService.generateToken();
        String secondToken = managementTokenService.generateToken();

        assertThat(firstToken).isNotEqualTo(secondToken);
    }

    @Test
    void hashesTokenUsingSha256HexadecimalFormat() {
        String rawToken = managementTokenService.generateToken();

        String hash = managementTokenService.hashToken(rawToken);

        assertThat(hash)
                .matches("^[0-9a-f]{64}$")
                .isNotEqualTo(rawToken);
    }

    @Test
    void hashingSameTokenProducesSameHash() {
        String rawToken = managementTokenService.generateToken();

        assertThat(managementTokenService.hashToken(rawToken))
                .isEqualTo(managementTokenService.hashToken(rawToken));
    }

    @Test
    void rejectsBlankRawToken() {
        assertThatThrownBy(() -> managementTokenService.hashToken("   "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("rawToken must not be blank");
    }
}
