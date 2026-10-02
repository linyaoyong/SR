package com.share.rental.admin;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContext;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
class AdminApplicationBoundaryTest {

    @Autowired
    private ApplicationContext context;

    @Test
    void adminApplication_doesNotRegisterOtherServiceControllersOrServices() {
        assertThat(context.containsBeanDefinition("authController")).isFalse();
        assertThat(context.containsBeanDefinition("userController")).isFalse();
        assertThat(context.containsBeanDefinition("categoryController")).isFalse();
        assertThat(context.containsBeanDefinition("itemController")).isFalse();
        assertThat(context.containsBeanDefinition("favoriteController")).isFalse();
        assertThat(context.containsBeanDefinition("messageFeignController")).isFalse();
        assertThat(context.containsBeanDefinition("userService")).isFalse();
        assertThat(context.containsBeanDefinition("itemService")).isFalse();
        assertThat(context.containsBeanDefinition("favoriteService")).isFalse();
        assertThat(context.containsBeanDefinition("systemNotificationService")).isFalse();
    }
}
