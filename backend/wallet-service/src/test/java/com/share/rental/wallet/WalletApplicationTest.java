package com.share.rental.wallet;

import com.baomidou.mybatisplus.extension.plugins.MybatisPlusInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.InnerInterceptor;
import com.baomidou.mybatisplus.extension.plugins.inner.OptimisticLockerInnerInterceptor;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.main.lazy-initialization=true"
})
class WalletApplicationTest {

    @Autowired
    private MybatisPlusInterceptor mybatisPlusInterceptor;

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

    @Test
    void contextProvidesOptimisticLockerInterceptor() {
        assertThat(mybatisPlusInterceptor.getInterceptors())
                .extracting(InnerInterceptor::getClass)
                .contains(OptimisticLockerInnerInterceptor.class);
    }
}
