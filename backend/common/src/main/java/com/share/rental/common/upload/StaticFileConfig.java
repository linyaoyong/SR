package com.share.rental.common.upload;

import org.springframework.boot.autoconfigure.condition.ConditionalOnClass;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Path;
import java.nio.file.Paths;

@Configuration
@ConditionalOnClass(WebMvcConfigurer.class)
public class StaticFileConfig implements WebMvcConfigurer {

    private final UploadProperties uploadProperties;

    public StaticFileConfig(UploadProperties uploadProperties) {
        this.uploadProperties = uploadProperties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        Path normalized = ImageUploadService.resolveUploadRoot(
                Paths.get("").toAbsolutePath().normalize(),
                uploadProperties.getRoot());
        String location = normalized.toUri().toString();
        if (!location.endsWith("/")) {
            location += "/";
        }
        registry.addResourceHandler("/files/**")
                .addResourceLocations(location);
    }
}
