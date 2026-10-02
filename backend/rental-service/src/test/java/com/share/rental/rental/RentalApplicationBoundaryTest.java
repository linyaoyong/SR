package com.share.rental.rental;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cloud.openfeign.EnableFeignClients;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = {
        "spring.cloud.nacos.discovery.enabled=false",
        "spring.main.lazy-initialization=true"
})
class RentalApplicationBoundaryTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void rentalApplicationScansOnlyRentalAndCommonPackages() {
        SpringBootApplication annotation = RentalApplication.class.getAnnotation(SpringBootApplication.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.scanBasePackages())
                .containsExactlyInAnyOrder("com.share.rental.rental", "com.share.rental.common");
    }

    @Test
    void rentalApplicationRegistersFeignClientsOnlyInRentalClientPackage() {
        EnableFeignClients annotation = RentalApplication.class.getAnnotation(EnableFeignClients.class);
        assertThat(annotation).isNotNull();
        assertThat(annotation.basePackages())
                .containsExactly("com.share.rental.rental.client");
    }

    @Test
    void rentalApplicationDoesNotRegisterOtherServiceBeans() {
        assertThat(context.containsBeanDefinition("authController")).isFalse();
        assertThat(context.containsBeanDefinition("userController")).isFalse();
        assertThat(context.containsBeanDefinition("itemController")).isFalse();
        assertThat(context.containsBeanDefinition("categoryController")).isFalse();
        assertThat(context.containsBeanDefinition("favoriteController")).isFalse();
        assertThat(context.containsBeanDefinition("walletController")).isFalse();
        assertThat(context.containsBeanDefinition("messageController")).isFalse();
        assertThat(context.containsBeanDefinition("messageFeignController")).isFalse();
        assertThat(context.containsBeanDefinition("adminController")).isFalse();
        assertThat(context.containsBeanDefinition("userService")).isFalse();
        assertThat(context.containsBeanDefinition("itemService")).isFalse();
        assertThat(context.containsBeanDefinition("walletService")).isFalse();
        assertThat(context.containsBeanDefinition("favoriteService")).isFalse();
        assertThat(context.containsBeanDefinition("systemNotificationService")).isFalse();
    }
}
