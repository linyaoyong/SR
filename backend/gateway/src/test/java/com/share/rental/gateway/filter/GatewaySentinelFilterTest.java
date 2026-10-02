package com.share.rental.gateway.filter;

import com.alibaba.csp.sentinel.slots.block.flow.FlowRule;
import com.alibaba.csp.sentinel.slots.block.flow.FlowRuleManager;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewaySentinelFilterTest {

    private final GatewaySentinelFilter filter = new GatewaySentinelFilter();

    @AfterEach
    void clearRules() {
        FlowRuleManager.loadRules(List.of());
    }

    @Test
    void itemListReturnsTooFrequentWhenFlowRuleBlocks() {
        FlowRule rule = new FlowRule("/api/items");
        rule.setCount(1);
        rule.setGrade(1);
        FlowRuleManager.loadRules(List.of(rule));

        MockServerWebExchange first = exchange(HttpMethod.GET, "/api/items");
        AtomicBoolean firstChainCalled = new AtomicBoolean(false);
        StepVerifier.create(filter.filter(first, chainCapturing(firstChainCalled, new AtomicReference<>())))
                .verifyComplete();

        MockServerWebExchange second = exchange(HttpMethod.GET, "/api/items");
        AtomicBoolean secondChainCalled = new AtomicBoolean(false);
        StepVerifier.create(filter.filter(second, chainCapturing(secondChainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(firstChainCalled).isTrue();
        assertThat(secondChainCalled).isFalse();
        assertThat(second.getResponse().getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
        assertThat(second.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseBody(second)).isEqualTo(
                "{\"code\":42900,\"message\":\"请求过于频繁\",\"data\":null}"
        );
    }

    @Test
    void itemDetailIsNotMatchedByItemListResource() {
        FlowRule rule = new FlowRule("/api/items");
        rule.setCount(0);
        rule.setGrade(1);
        FlowRuleManager.loadRules(List.of(rule));

        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/items/1");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    private MockServerWebExchange exchange(HttpMethod method, String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build());
    }

    private WebFilterChain chainCapturing(AtomicBoolean called, AtomicReference<ServerWebExchange> capturedExchange) {
        return exchange -> {
            called.set(true);
            capturedExchange.set(exchange);
            return Mono.empty();
        };
    }

    private String responseBody(MockServerWebExchange exchange) {
        List<String> chunks = exchange.getResponse().getBodyAsString()
                .map(value -> List.of(value))
                .block(Duration.ofSeconds(1));
        return String.join("", chunks == null ? List.of() : chunks);
    }
}
