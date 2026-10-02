package com.share.rental.common.ingress;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.security.SecurityProperties;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.boot.autoconfigure.condition.ConditionalOnWebApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Bean;
import org.springframework.http.MediaType;
import org.springframework.web.servlet.HandlerInterceptor;
import org.springframework.web.servlet.handler.MappedInterceptor;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.util.Collections;
import java.util.List;

/** The gateway and Feign clients authenticate before any backend handler trusts identity headers. */
@Configuration(proxyBeanMethods = false)
@EnableConfigurationProperties(SecurityProperties.class)
@ConditionalOnWebApplication(type = ConditionalOnWebApplication.Type.SERVLET)
public class BackendIngressConfig implements WebMvcConfigurer {
    private final byte[] expectedCredential;
    private final ObjectMapper objectMapper = new ObjectMapper();

    public BackendIngressConfig(SecurityProperties properties) {
        String credential = properties.getInternalToken();
        if (credential == null || credential.isBlank()) {
            throw new IllegalArgumentException("sr.security.internal-token must be configured");
        }
        expectedCredential = credential.getBytes(StandardCharsets.UTF_8);
    }

    // A bean is discovered by both MVC and Actuator mappings; the MVC registry alone misses Actuator.
    @Bean
    public MappedInterceptor backendIngressInterceptor() {
        return new MappedInterceptor(
                new String[]{"/api", "/api/**", "/internal", "/internal/**", "/files", "/files/**",
                        "/actuator", "/actuator/**"},
                new String[]{"/actuator/health", "/actuator/health/**"},
                new HandlerInterceptor() {
            @Override
            public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler)
                    throws Exception {
                List<String> credentials = Collections.list(request.getHeaders("X-Internal-Token"));
                if (credentials.size() == 1 && MessageDigest.isEqual(expectedCredential,
                        credentials.get(0).getBytes(StandardCharsets.UTF_8))) {
                    return true;
                }
                response.setStatus(HttpServletResponse.SC_FORBIDDEN);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                response.setCharacterEncoding(StandardCharsets.UTF_8.name());
                objectMapper.writeValue(response.getWriter(), ApiResponse.error(ErrorCode.INTERNAL_FORBIDDEN));
                return false;
            }
        });
    }
}
