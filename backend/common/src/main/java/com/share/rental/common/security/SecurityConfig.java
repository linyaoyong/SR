package com.share.rental.common.security;

import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Lazy;

@Configuration
@EnableConfigurationProperties(SecurityProperties.class)
public class SecurityConfig {

    @Bean
    @Lazy(false)
    public JwtUtil jwtUtil(SecurityProperties securityProperties) {
        return new JwtUtil(securityProperties.getJwtSecret());
    }
}
