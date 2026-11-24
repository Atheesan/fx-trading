package com.fxai.auth;

import org.junit.jupiter.api.Test;
import static org.assertj.core.api.Assertions.*;

class AuthUnitTests {
    @Test void passwordsAreSaltedAndNeverStoredAsPlaintext() {
        var encoder = new TokenConfiguration().passwordEncoder();
        String first = encoder.encode("a-long-secret-password");
        String second = encoder.encode("a-long-secret-password");
        assertThat(first).doesNotContain("a-long-secret-password").isNotEqualTo(second);
        assertThat(encoder.matches("a-long-secret-password", first)).isTrue();
        assertThat(encoder.matches("incorrect", first)).isFalse();
    }
    @Test void signingKeyMustHaveEnoughEntropy() {
        assertThatThrownBy(() -> new TokenConfiguration().signingKey(new AuthProperties("c2hvcnQ=", true)))
                .isInstanceOf(IllegalArgumentException.class);
    }
    @Test void normalizesEmailAndHashesRefreshTokens() {
        assertThat(AuthService.normalize(" Person@Example.COM ")).isEqualTo("person@example.com");
        assertThat(AuthService.hash("secret")).hasSize(64).doesNotContain("secret");
    }
}
