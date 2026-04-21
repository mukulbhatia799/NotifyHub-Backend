package com.notifyhub;

import com.notifyhub.security.JwtUtil;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.security.core.userdetails.User;
import org.springframework.security.core.userdetails.UserDetails;

import java.util.Collections;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("JwtUtil — token generation and validation")
class JwtUtilTest {

    // 256-bit Base64 encoded secret (safe for HS256)
    private static final String SECRET =
            "404E635266556A586E3272357538782F413F4428472B4B6250645367566B5970";
    private static final long EXPIRATION_MS = 900_000L; // 15 min

    private JwtUtil     jwtUtil;
    private UserDetails userDetails;

    @BeforeEach
    void setUp() {
        jwtUtil     = new JwtUtil(SECRET, EXPIRATION_MS);
        userDetails = User.withUsername("user@example.com")
                .password("irrelevant")
                .authorities(Collections.emptyList())
                .build();
    }

    @Test
    @DisplayName("Generated token is non-null and contains expected subject")
    void generateToken_returnsValidToken() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(token).isNotBlank();
        assertThat(jwtUtil.extractUsername(token)).isEqualTo("user@example.com");
    }

    @Test
    @DisplayName("isTokenValid returns true for matching user and unexpired token")
    void isTokenValid_returnsTrue_forValidToken() {
        String token = jwtUtil.generateToken(userDetails);

        assertThat(jwtUtil.isTokenValid(token, userDetails)).isTrue();
    }

    @Test
    @DisplayName("isTokenValid returns false when username does not match")
    void isTokenValid_returnsFalse_forDifferentUser() {
        String token = jwtUtil.generateToken(userDetails);

        UserDetails other = User.withUsername("other@example.com")
                .password("x")
                .authorities(Collections.emptyList())
                .build();

        assertThat(jwtUtil.isTokenValid(token, other)).isFalse();
    }

    @Test
    @DisplayName("isTokenExpired returns false for a fresh token")
    void isTokenExpired_returnsFalse_forFreshToken() {
        String token = jwtUtil.generateToken(userDetails);
        assertThat(jwtUtil.isTokenExpired(token)).isFalse();
    }

    @Test
    @DisplayName("Expired token throws exception on parsing")
    void expiredToken_throwsException() {
        JwtUtil shortLived = new JwtUtil(SECRET, -1L); // immediately expired
        String  token      = shortLived.generateToken(userDetails);

        // Parsing an expired token should throw
        assertThatThrownBy(() -> shortLived.isTokenExpired(token))
                .isInstanceOf(Exception.class);
    }

    @Test
    @DisplayName("getExpirationMs returns the configured value")
    void getExpirationMs_returnsConfiguredValue() {
        assertThat(jwtUtil.getExpirationMs()).isEqualTo(EXPIRATION_MS);
    }
}
