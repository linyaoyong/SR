package com.share.rental.auth;

import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThat;

class AdminPasswordBcryptTest {

    private static final String STORED_HASH =
            "$2a$10$BWjZ/5bA4mJ0AlMNWAZ2V.lXNgZeCZUAl0ZHWgCl3dbr4Xw.j1RoC";

    @Test
    void adminDefaultPasswordMatches123456() {
        BCryptPasswordEncoder encoder = new BCryptPasswordEncoder();
        assertThat(encoder.matches("123456", STORED_HASH)).isTrue();
    }
}
