package com.share.rental.common.feign;

import com.share.rental.common.security.SecurityProperties;
import feign.RequestInterceptor;
import feign.RequestTemplate;
import org.springframework.util.StringUtils;

public class InternalTokenInterceptor implements RequestInterceptor {

    public static final String INTERNAL_TOKEN_HEADER = "X-Internal-Token";

    private final SecurityProperties securityProperties;

    public InternalTokenInterceptor(SecurityProperties securityProperties) {
        this.securityProperties = securityProperties;
    }

    @Override
    public void apply(RequestTemplate template) {
        String internalToken = securityProperties.getInternalToken();
        if (StringUtils.hasText(internalToken)) {
            template.removeHeader(INTERNAL_TOKEN_HEADER);
            template.header(INTERNAL_TOKEN_HEADER, internalToken);
        }
    }
}
