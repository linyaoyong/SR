package com.share.rental.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureMockMvc
@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.main.lazy-initialization=true",
        "management.health.defaults.enabled=false"
})
class ActuatorIngressTest {
    @Autowired
    private MockMvc mvc;

    @Test
    void realActuatorInfoRejectsMissingWrongAndDuplicateCredentials() throws Exception {
        mvc.perform(get("/actuator/info")).andExpect(status().isForbidden());
        mvc.perform(get("/actuator/info").header("X-Internal-Token", "wrong-test-token"))
                .andExpect(status().isForbidden());
        mvc.perform(get("/actuator/info").header("X-Internal-Token",
                        "test-only-backend-ingress-token", "wrong-test-token"))
                .andExpect(status().isForbidden());
    }

    @Test
    void realActuatorInfoAllowsGatewayCredentialAndHealthRemainsPublic() throws Exception {
        var result = mvc.perform(get("/actuator/info")
                        .header("X-Internal-Token", "test-only-backend-ingress-token"))
                .andExpect(status().isOk()).andReturn();
        assertThat(((org.springframework.web.method.HandlerMethod) result.getHandler()).getBeanType().getName())
                .contains("WebMvcEndpointHandlerMapping");
        mvc.perform(get("/actuator/health")).andExpect(status().isOk());
    }
}
