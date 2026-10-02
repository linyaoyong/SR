package com.share.rental.auth;

import com.share.rental.common.security.SecurityConfig;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.config.annotation.EnableWebMvc;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class BackendIngressTest {
    private static final String TEST_TOKEN = "test-only-backend-ingress-token";
    private final WebApplicationContextRunner runner = new WebApplicationContextRunner()
            .withUserConfiguration(ProbeConfig.class)
            .withPropertyValues("sr.security.jwt-secret=test-only-jwt-secret-at-least-32-bytes",
                    "sr.security.internal-token=" + TEST_TOKEN);

    @Test
    void forgedIdentityCannotCallBackendWithoutCredential() {
        runner.run(context -> {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            mvc.perform(get("/api/users/me").header("X-User-Id", "999").header("X-User-Role", "ADMIN"))
                    .andExpect(status().isForbidden());
        });
    }

    @Test
    void internalApiAndFilesRejectMissingWrongAndDuplicateCredentials() {
        runner.run(context -> {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            for (String path : new String[]{"/internal/users/1", "/files/items/test.jpg", "/api/auth/login", "/actuator/info"}) {
                mvc.perform(get(path)).andExpect(status().isForbidden());
                mvc.perform(get(path).header("X-Internal-Token", "wrong-test-token"))
                        .andExpect(status().isForbidden());
                mvc.perform(get(path).header("X-Internal-Token", TEST_TOKEN, "wrong-test-token"))
                        .andExpect(status().isForbidden());
            }
        });
    }

    @Test
    void configuredCredentialAllowsGatewayAndFeignRequests() {
        runner.run(context -> {
            MockMvc mvc = MockMvcBuilders.webAppContextSetup(context).build();
            for (String path : new String[]{"/api/users/me", "/internal/users/1", "/files/items/test.jpg", "/api/auth/login"}) {
                mvc.perform(get(path).header("X-Internal-Token", TEST_TOKEN).header("X-User-Id", "1"))
                        .andExpect(status().isOk());
            }
            mvc.perform(get("/actuator/health")).andExpect(status().isOk());
        });
    }

    @Configuration(proxyBeanMethods = false)
    @EnableWebMvc
    @ComponentScan("com.share.rental.common.ingress")
    @Import({SecurityConfig.class, ProbeController.class})
    static class ProbeConfig {}

    @RestController
    static class ProbeController {
        @GetMapping({"/api/users/me", "/internal/users/1", "/files/items/test.jpg", "/api/auth/login", "/actuator/health", "/actuator/info"})
        String reachable() { return "reachable"; }
    }
}
