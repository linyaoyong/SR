package com.share.rental.common.upload;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@ConditionalOnClass(WebMvcConfigurer.class)
@EnableConfigurationProperties(UploadProperties.class)
public class UploadConfig {

    @Bean
    @ConditionalOnClass(WebMvcConfigurer.class)
    public ImageUploadService imageUploadService(UploadProperties uploadProperties) {
        return new ImageUploadService(uploadProperties);
    }
}
