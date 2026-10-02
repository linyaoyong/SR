package com.share.rental.gateway.config;

import com.alibaba.csp.sentinel.adapter.spring.webflux.callback.WebFluxCallbackManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.reactive.function.server.ServerResponse;
import reactor.test.StepVerifier;

import static org.assertj.core.api.Assertions.assertThat;

class SentinelWebFluxBlockHandlerConfigTest {

    @AfterEach
    void resetBlockHandler() {
        WebFluxCallbackManager.resetBlockHandler();
    }

    @Test
    void registersJsonTooManyRequestsBlockHandler() {
        new SentinelWebFluxBlockHandlerConfig().registerBlockHandler();
        MockServerWebExchange exchange = MockServerWebExchange.from(
                MockServerHttpRequest.get("/api/items").build());

        StepVerifier.create(WebFluxCallbackManager.getBlockHandler()
                        .handleRequest(exchange, new RuntimeException("blocked")))
                .assertNext(response -> {
                    assertThat(response.statusCode()).isEqualTo(HttpStatusCode.valueOf(429));
                    assertThat(response.headers().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
                    assertThat(response).isInstanceOf(ServerResponse.class);
                })
                .verifyComplete();
    }
}
