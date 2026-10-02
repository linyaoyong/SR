package com.share.rental.message;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.main.lazy-initialization=true"
})
class MessageApplicationTest {

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.context.ApplicationContext ingressContext;

    @Test
    void backendLoadsSharedIngressGuard() {
        org.assertj.core.api.Assertions.assertThat(ingressContext.containsBean("backendIngressConfig")).isTrue();
    }

    @org.springframework.beans.factory.annotation.Autowired
    private org.springframework.core.env.Environment ingressEnvironment;

    @Test
    void localServiceBindingAndDiscoveryUseLoopback() {
        org.assertj.core.api.Assertions.assertThat(ingressEnvironment.getProperty("server.address"))
                .isEqualTo("127.0.0.1");
        org.assertj.core.api.Assertions.assertThat(ingressEnvironment.getProperty("spring.cloud.nacos.discovery.ip"))
                .isEqualTo("127.0.0.1");
    }

    @Test
    void contextLoads() {
    }
}
