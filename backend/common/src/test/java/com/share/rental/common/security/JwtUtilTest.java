package com.share.rental.common.security;

import com.share.rental.common.exception.BusinessException;
import org.junit.jupiter.api.Test;

import java.time.Duration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class JwtUtilTest {

    private final JwtUtil jwtUtil = new JwtUtil("test-only-jwt-secret-at-least-32-bytes");

    @Test
    void createTokenCanBeParsedBackToClaims() {
        String token = jwtUtil.createToken(7L, "senjing", "USER", Duration.ofMinutes(5));

        JwtClaims claims = jwtUtil.parse(token);

        assertThat(claims.userId()).isEqualTo(7L);
        assertThat(claims.username()).isEqualTo("senjing");
        assertThat(claims.role()).isEqualTo("USER");
    }

    @Test
    void parseBadTokenThrowsUnauthorizedBusinessException() {
        assertThatThrownBy(() -> jwtUtil.parse("bad-token"))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未登录或登录已过期");
    }

    @Test
    void parseExpiredTokenThrowsUnauthorizedBusinessException() {
        String token = jwtUtil.createToken(7L, "senjing", "USER", Duration.ofSeconds(-1));

        assertThatThrownBy(() -> jwtUtil.parse(token))
                .isInstanceOf(BusinessException.class)
                .hasMessage("未登录或登录已过期");
    }

    @Test
    void constructorRejectsBlankSecretWithConfigMessage() {
        assertThatThrownBy(() -> new JwtUtil(" "))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sr.security.jwt-secret must contain at least 32 bytes");
    }

    @Test
    void constructorRejectsShortSecretWithConfigMessage() {
        assertThatThrownBy(() -> new JwtUtil("short"))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessage("sr.security.jwt-secret must contain at least 32 bytes");
    }
}
