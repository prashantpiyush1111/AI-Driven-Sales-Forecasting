package com.salesforecast.security;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private static final String SECRET = "unit-test-secret-key-with-at-least-32-chars!";
    private static final String OTHER_SECRET = "another-different-secret-key-of-32-plus-chars";
    private static final long ONE_HOUR_MS = 3_600_000L;

    private final JwtUtil jwtUtil = new JwtUtil(SECRET, ONE_HOUR_MS);

    @Test
    void generateToken_containsUsernameAndRole() {
        String token = jwtUtil.generateToken("prashant", "ADMIN");

        assertThat(jwtUtil.extractUsername(token)).isEqualTo("prashant");
        assertThat(jwtUtil.extractRole(token)).isEqualTo("ADMIN");
    }

    @Test
    void isTokenValid_returnsTrue_forMatchingUsername() {
        String token = jwtUtil.generateToken("prashant", "USER");

        assertThat(jwtUtil.isTokenValid(token, "prashant")).isTrue();
    }

    @Test
    void isTokenValid_returnsFalse_forDifferentUsername() {
        String token = jwtUtil.generateToken("prashant", "USER");

        assertThat(jwtUtil.isTokenValid(token, "someone-else")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forExpiredToken() {
        JwtUtil alreadyExpiredIssuer = new JwtUtil(SECRET, -1_000L);
        String expiredToken = alreadyExpiredIssuer.generateToken("prashant", "USER");

        assertThat(jwtUtil.isTokenValid(expiredToken, "prashant")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forTokenSignedWithDifferentSecret() {
        String forgedToken = new JwtUtil(OTHER_SECRET, ONE_HOUR_MS).generateToken("prashant", "ADMIN");

        assertThat(jwtUtil.isTokenValid(forgedToken, "prashant")).isFalse();
    }

    @Test
    void isTokenValid_returnsFalse_forGarbageToken() {
        assertThat(jwtUtil.isTokenValid("not-a-real-jwt", "prashant")).isFalse();
    }

    @Test
    void constructor_throws_whenSecretIsTooShort() {
        assertThatThrownBy(() -> new JwtUtil("short-secret", ONE_HOUR_MS))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("at least 32");
    }
}