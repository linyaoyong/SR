package com.share.rental.common.feign;

import com.share.rental.common.security.SecurityProperties;
import feign.RequestTemplate;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class InternalTokenInterceptorTest {

    @Test
    void replacesExistingInternalTokenHeader() {
        SecurityProperties properties = new SecurityProperties();
        properties.setInternalToken("expected-token");
        RequestTemplate template = new RequestTemplate();
        template.header(InternalTokenInterceptor.INTERNAL_TOKEN_HEADER, "old-token");

        new InternalTokenInterceptor(properties).apply(template);

        assertThat(template.headers().get(InternalTokenInterceptor.INTERNAL_TOKEN_HEADER))
                .containsExactly("expected-token");
    }
}
