package com.share.rental.gateway;

import com.share.rental.common.exception.ErrorCode;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.reactive.AutoConfigureWebTestClient;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.env.Environment;
import org.springframework.util.unit.DataSize;
import org.springframework.test.web.reactive.server.WebTestClient;

import static org.assertj.core.api.Assertions.assertThat;

@AutoConfigureWebTestClient
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.main.web-application-type=reactive"
})
class GatewayApplicationTest {

    @Autowired
    private WebTestClient webTestClient;

    @Autowired
    private Environment environment;

    @Autowired
    private org.springframework.context.ApplicationContext context;

    @Test
    void gatewayDoesNotLoadServletIngressGuard() {
        assertThat(context.containsBean("backendIngressConfig")).isFalse();
    }

    @Test
    void contextLoads() {
    }

    @Test
    void internalPathIsBlockedBeforeRouteMatching() {
        webTestClient.get()
                .uri("/internal/users/1/public")
                .exchange()
                .expectStatus().isForbidden()
                .expectBody()
                .jsonPath("$.code").isEqualTo(ErrorCode.INTERNAL_FORBIDDEN.code())
                .jsonPath("$.message").isEqualTo(ErrorCode.INTERNAL_FORBIDDEN.message());
    }

    @Test
    void codecLimitAllowsImageUploadProxying() {
        String configured = environment.getProperty("spring.codec.max-in-memory-size", "256KB");
        assertThat(DataSize.parse(configured).toBytes()).isGreaterThan(1024L * 1024L);
    }
}
