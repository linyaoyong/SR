package com.share.rental.common.feign;

import com.share.rental.common.security.JwtUtil;
import com.share.rental.common.security.SecurityProperties;
import feign.RequestInterceptor;
import org.springframework.context.annotation.Bean;

public class FeignConfig {

    @Bean
    public RequestInterceptor internalTokenInterceptor(SecurityProperties securityProperties) {
        return new InternalTokenInterceptor(securityProperties);
    }
}
