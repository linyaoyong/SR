package com.share.rental.gateway.config;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.WebFluxCallbackManager;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import jakarta.annotation.PostConstruct;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.server.ServerResponse;

@Configuration
public class SentinelWebFluxBlockHandlerConfig {

    @PostConstruct
    void registerBlockHandler() {
        WebFluxCallbackManager.setBlockHandler((exchange, throwable) ->
                ServerResponse.status(429)
                        .contentType(MediaType.APPLICATION_JSON)
                        .bodyValue(ApiResponse.error(ErrorCode.REQUEST_TOO_FREQUENT)));
    }
}
