package com.share.rental.common.security;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.assertj.core.api.Assertions.assertThat;

class SecurityConfigTest {
    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(SecurityConfig.class);

    @Test
    void secretsHaveNoBuiltInDefaults() {
        SecurityProperties properties = new SecurityProperties();
        assertThat(properties.getJwtSecret()).isNull();
        assertThat(properties.getInternalToken()).isNull();
    }

    @Test
    void missingOrBlankInternalCredentialFailsStartup() {
        runner.withPropertyValues("sr.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes")
                .run(context -> assertThat(context).hasFailed());
        runner.withPropertyValues("sr.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes",
                        "sr.security.internal-token= ")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void missingJwtSecretFailsStartup() {
        runner.withPropertyValues("sr.security.internal-token=test-only-internal-credential")
                .run(context -> assertThat(context).hasFailed());
    }

    @Test
    void explicitTestSecretsAllowStartup() {
        runner.withPropertyValues("sr.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes",
                        "sr.security.internal-token=test-only-internal-credential")
                .run(context -> assertThat(context).hasNotFailed());
    }
}
