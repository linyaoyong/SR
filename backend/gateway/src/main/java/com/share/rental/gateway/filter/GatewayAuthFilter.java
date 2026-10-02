package com.share.rental.gateway.filter;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.BusinessException;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import com.share.rental.common.security.JwtClaims;
import com.share.rental.common.security.JwtUtil;
import com.share.rental.common.security.SecurityProperties;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.stereotype.Component;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GatewayAuthFilter implements WebFilter, Ordered {

    private static final String USER_ID_HEADER = "X-User-Id";
    private static final String USER_ROLE_HEADER = "X-User-Role";

    private final JwtUtil jwtUtil;
    private final ObjectMapper objectMapper;
    private final String internalToken;

    @Autowired
    public GatewayAuthFilter(JwtUtil jwtUtil, SecurityProperties securityProperties) {
        this(jwtUtil, securityProperties, new ObjectMapper());
    }

    GatewayAuthFilter(JwtUtil jwtUtil, SecurityProperties securityProperties, ObjectMapper objectMapper) {
        this.jwtUtil = jwtUtil;
        this.objectMapper = objectMapper;
        this.internalToken = securityProperties.getInternalToken();
        if (internalToken == null || internalToken.isBlank()) {
            throw new IllegalArgumentException("sr.security.internal-token must be configured");
        }
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        ServerWebExchange sanitizedExchange = sanitizeUserHeaders(exchange);
        ServerHttpRequest request = sanitizedExchange.getRequest();
        String path = request.getPath().pathWithinApplication().value();
        HttpMethod method = request.getMethod();

        // Gateway/MVC decode path segments and discard matrix parameters when routing.
        // SR routes use numeric IDs and generated filenames; reject ambiguous raw forms
        // before comparing authorization paths, while leaving encoded query parameters valid.
        if (hasAmbiguousPath(request.getURI().getRawPath())) {
            return writeError(sanitizedExchange, HttpStatus.BAD_REQUEST, ErrorCode.BAD_REQUEST);
        }

        if (isInternalPath(path)) {
            return writeError(sanitizedExchange, HttpStatus.FORBIDDEN, ErrorCode.INTERNAL_FORBIDDEN);
        }

        if (HttpMethod.OPTIONS.equals(method)) {
            return chain.filter(sanitizedExchange);
        }

        String token = resolveToken(request, path);

        // refresh 接口带的是 refresh token 而非 JWT，不在此解析，直接放行交由 auth-service 校验
        boolean skipJwtParse = path.equals("/api/auth/refresh");
        if (isAnonymousAllowed(method, path) && (skipJwtParse || token == null || token.isBlank())) {
            return chain.filter(sanitizedExchange);
        }

        if (token == null || token.isBlank()) {
            return writeError(sanitizedExchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
        }

        JwtClaims claims;
        try {
            claims = jwtUtil.parse(token);
        } catch (BusinessException ex) {
            return writeError(sanitizedExchange, HttpStatus.UNAUTHORIZED, ErrorCode.UNAUTHORIZED);
        }

        if (isAdminPath(path) && !"ADMIN".equals(claims.role())) {
            return writeError(sanitizedExchange, HttpStatus.FORBIDDEN, ErrorCode.FORBIDDEN);
        }

        ServerHttpRequest authenticatedRequest = request.mutate()
                .headers(headers -> {
                    headers.set(USER_ID_HEADER, String.valueOf(claims.userId()));
                    headers.set(USER_ROLE_HEADER, claims.role());
                })
                .build();
        return chain.filter(sanitizedExchange.mutate().request(authenticatedRequest).build());
    }

    @Override
    public int getOrder() {
        return -100;
    }

    private boolean isInternalPath(String path) {
        return path.equals("/internal") || path.startsWith("/internal/");
    }

    private boolean hasAmbiguousPath(String path) {
        if (path.contains("%") || path.contains(";") || path.contains("//")) {
            return true;
        }
        for (String segment : path.split("/")) {
            if (".".equals(segment) || "..".equals(segment)) {
                return true;
            }
        }
        return false;
    }

    private ServerWebExchange sanitizeUserHeaders(ServerWebExchange exchange) {
        ServerHttpRequest sanitizedRequest = exchange.getRequest().mutate()
                .headers(headers -> {
                    headers.remove(USER_ID_HEADER);
                    headers.remove(USER_ROLE_HEADER);
                    headers.remove("X-Internal-Token");
                    headers.set("X-Internal-Token", internalToken);
                })
                .build();
        return exchange.mutate().request(sanitizedRequest).build();
    }

    private boolean isAdminPath(String path) {
        return path.equals("/api/admin") || path.startsWith("/api/admin/");
    }

    private boolean isAnonymousAllowed(HttpMethod method, String path) {
        if (path.equals("/actuator") || path.startsWith("/actuator/")) {
            return true;
        }

        if (HttpMethod.POST.equals(method)) {
            return path.equals("/api/auth/register")
                    || path.equals("/api/auth/login")
                    || path.equals("/api/auth/admin/login")
                    || path.equals("/api/auth/refresh");
        }

        if (!HttpMethod.GET.equals(method)) {
            return false;
        }

        return path.equals("/api/categories")
                || path.startsWith("/api/categories/")
                || path.equals("/api/items")
                || isSingleSegmentUnder(path, "/api/items/")
                || isPublicUserProfile(path)
                || isPublicRentalUserData(path)
                || isPublicRentalItemData(path)
                || path.startsWith("/files/avatars/")
                || path.startsWith("/files/items/")
                || path.startsWith("/files/messages/")
                || path.startsWith("/files/reviews/")
                || path.startsWith("/files/audit/");
    }

    private boolean isPublicUserProfile(String path) {
        if (!isSingleSegmentUnder(path, "/api/users/")) {
            return false;
        }
        String id = path.substring("/api/users/".length());
        return id.matches("\\d+");
    }

    private boolean isPublicRentalUserData(String path) {
        String prefix = "/api/rentals/users/";
        if (!path.startsWith(prefix)) {
            return false;
        }
        String remaining = path.substring(prefix.length());
        return remaining.matches("\\d+/(history|reviews)");
    }

    private boolean isPublicRentalItemData(String path) {
        String prefix = "/api/rentals/items/";
        if (!path.startsWith(prefix)) {
            return false;
        }
        String remaining = path.substring(prefix.length());
        return remaining.matches("\\d+/reviews");
    }

    private boolean isSingleSegmentUnder(String path, String prefix) {
        if (!path.startsWith(prefix)) {
            return false;
        }
        String remaining = path.substring(prefix.length());
        return !remaining.isBlank() && !remaining.contains("/");
    }

    private String resolveToken(ServerHttpRequest request, String path) {
        String authorization = request.getHeaders().getFirst(HttpHeaders.AUTHORIZATION);
        if (authorization != null && authorization.startsWith("Bearer ")) {
            return authorization.substring("Bearer ".length()).trim();
        }
        if (path.equals("/ws") || path.startsWith("/ws/")) {
            return request.getQueryParams().getFirst("token");
        }
        return null;
    }

    private Mono<Void> writeError(ServerWebExchange exchange, HttpStatus status, ErrorCode errorCode) {
        exchange.getResponse().setStatusCode(status);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = toJson(ApiResponse.error(errorCode)).getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String toJson(ApiResponse<?> response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            return "{\"code\":" + ErrorCode.SYSTEM_ERROR.code()
                    + ",\"message\":\"" + ErrorCode.SYSTEM_ERROR.message()
                    + "\",\"data\":null}";
        }
    }
}
