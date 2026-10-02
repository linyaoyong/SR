package com.share.rental.gateway.filter;

import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.security.JwtUtil;
import com.share.rental.common.security.SecurityProperties;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.mock.http.server.reactive.MockServerHttpRequest;
import org.springframework.mock.web.server.MockServerWebExchange;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;

class GatewayAuthFilterTest {

    private static final String JWT_SECRET = "test-only-jwt-secret-at-least-32-bytes";

    private final JwtUtil jwtUtil = new JwtUtil(JWT_SECRET);
    private final GatewayAuthFilter filter = new GatewayAuthFilter(jwtUtil, testSecurityProperties());

    private static SecurityProperties testSecurityProperties() {
        SecurityProperties properties = new SecurityProperties();
        properties.setInternalToken("test-only-gateway-internal-token");
        return properties;
    }

    @org.junit.jupiter.params.ParameterizedTest
    @org.junit.jupiter.params.provider.ValueSource(strings = {"/api/%61dmin/dashboard", "/api/admin;probe/dashboard"})
    void ambiguousAdminPathsCannotBypassRoleAuthorization(String path) {
        String token = jwtUtil.createToken(7L, "alice", "USER", Duration.ofHours(1));
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.GET, java.net.URI.create("http://localhost" + path))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token).build());
        // These raw paths really match the admin route under Spring's decoded path semantics.
        assertThat(new org.springframework.web.util.pattern.PathPatternParser().parse("/api/admin/**")
                .matches(exchange.getRequest().getPath().pathWithinApplication())).isTrue();
        AtomicBoolean forwarded = new AtomicBoolean(false);
        StepVerifier.create(filter.filter(exchange, chainCapturing(forwarded, new AtomicReference<>())))
                .verifyComplete();
        assertThat(forwarded).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void internalPathReturnsForbidden() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/internal/users/1/public");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseBody(exchange)).isEqualTo(
                "{\"code\":40003,\"message\":\"禁止外部访问内部接口\",\"data\":null}"
        );
    }

    @Test
    void publicItemListWithoutTokenCallsChain() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/items");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void publicItemReviewsWithoutTokenCallsChain() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/rentals/items/10/reviews");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void internalOptionsPreflightReturnsForbidden() {
        MockServerWebExchange exchange = exchange(HttpMethod.OPTIONS, "/internal/users/1/public");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void nonInternalOptionsPreflightCallsChain() {
        MockServerWebExchange exchange = exchange(HttpMethod.OPTIONS, "/api/users/me");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(exchange.getResponse().getStatusCode()).isNull();
    }

    @Test
    void anonymousPublicRequestStripsSpoofedUserHeaders() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .method(HttpMethod.GET, "/api/items")
                .header("X-User-Id", "999")
                .header("X-User-Role", "ADMIN")
                .build());
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, capturedExchange)))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Id")).isFalse();
        assertThat(capturedExchange.get().getRequest().getHeaders().containsKey("X-User-Role")).isFalse();
    }

    @Test
    void protectedUserMeWithoutTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/users/me");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseBody(exchange)).isEqualTo(
                "{\"code\":40001,\"message\":\"未登录或登录已过期\",\"data\":null}"
        );
    }

    @Test
    void adminPathWithUserTokenReturnsForbidden() {
        String token = jwtUtil.createToken(7L, "alice", "USER", Duration.ofHours(1));
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/admin/dashboard", token);
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseBody(exchange)).isEqualTo(
                "{\"code\":40002,\"message\":\"无权限\",\"data\":null}"
        );
    }

    @Test
    void publicItemDetailWithTokenInjectsOptionalUserHeaders() {
        String token = jwtUtil.createToken(12L, "carol", "USER", Duration.ofHours(1));
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/items/100", token);
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, capturedExchange)))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("12");
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
    }

    @Test
    void userBlacklistWithoutTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/users/blacklist");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseBody(exchange)).isEqualTo(
                "{\"code\":40001,\"message\":\"未登录或登录已过期\",\"data\":null}"
        );
    }

    @Test
    void itemImagesWithoutTokenReturnsUnauthorized() {
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/items/100/images");
        AtomicBoolean chainCalled = new AtomicBoolean(false);

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, new AtomicReference<>())))
                .verifyComplete();

        assertThat(chainCalled).isFalse();
        assertThat(exchange.getResponse().getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        assertThat(exchange.getResponse().getHeaders().getContentType()).isEqualTo(MediaType.APPLICATION_JSON);
        assertThat(responseBody(exchange)).isEqualTo(
                "{\"code\":40001,\"message\":\"未登录或登录已过期\",\"data\":null}"
        );
    }

    @Test
    void adminPathWithAdminTokenInjectsUserHeaders() {
        String token = jwtUtil.createToken(9L, "root", "ADMIN", Duration.ofHours(1));
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/admin/dashboard", token);
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, capturedExchange)))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("9");
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("ADMIN");
    }

    @Test
    void websocketTokenQueryParamInjectsUserId() {
        String token = jwtUtil.createToken(11L, "bob", "USER", Duration.ofHours(1));
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/ws/chat?token=" + token);
        AtomicBoolean chainCalled = new AtomicBoolean(false);
        AtomicReference<ServerWebExchange> capturedExchange = new AtomicReference<>();

        StepVerifier.create(filter.filter(exchange, chainCapturing(chainCalled, capturedExchange)))
                .verifyComplete();

        assertThat(chainCalled).isTrue();
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Id")).isEqualTo("11");
        assertThat(capturedExchange.get().getRequest().getHeaders().getFirst("X-User-Role")).isEqualTo("USER");
    }

    @Test
    void clientInternalCredentialIsReplacedOnPublicLogin() {
        MockServerWebExchange exchange = MockServerWebExchange.from(MockServerHttpRequest
                .post("/api/auth/login")
                .header("X-Internal-Token", "client-forged-token", "another-forged-token")
                .build());
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        StepVerifier.create(filter.filter(exchange, chainCapturing(new AtomicBoolean(), captured))).verifyComplete();
        assertThat(captured.get().getRequest().getHeaders().get("X-Internal-Token"))
                .containsExactly("test-only-gateway-internal-token");
    }

    @Test
    void authenticatedForwardingIncludesGatewayCredential() {
        String jwt = jwtUtil.createToken(12L, "carol", "USER", Duration.ofHours(1));
        MockServerWebExchange exchange = exchange(HttpMethod.GET, "/api/users/me", jwt);
        AtomicReference<ServerWebExchange> captured = new AtomicReference<>();
        StepVerifier.create(filter.filter(exchange, chainCapturing(new AtomicBoolean(), captured))).verifyComplete();
        assertThat(captured.get().getRequest().getHeaders().get("X-Internal-Token"))
                .containsExactly("test-only-gateway-internal-token");
    }

    private MockServerWebExchange exchange(HttpMethod method, String path) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path).build());
    }

    private MockServerWebExchange exchange(HttpMethod method, String path, String token) {
        return MockServerWebExchange.from(MockServerHttpRequest.method(method, path)
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .build());
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
