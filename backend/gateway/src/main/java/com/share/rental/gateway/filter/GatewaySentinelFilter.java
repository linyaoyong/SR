package com.share.rental.gateway.filter;

import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.share.rental.common.exception.ErrorCode;
import com.share.rental.common.response.ApiResponse;
import org.springframework.core.Ordered;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;

@Component
public class GatewaySentinelFilter implements WebFilter, Ordered {

    public static final String ITEM_LIST_RESOURCE = "/api/items";
    public static final String APPLICATION_CREATE_RESOURCE = "/api/rentals/applications";

    private final ObjectMapper objectMapper;

    public GatewaySentinelFilter() {
        this(new ObjectMapper());
    }

    GatewaySentinelFilter(ObjectMapper objectMapper) {
        this.objectMapper = objectMapper;
    }

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        String resource = resourceName(exchange);
        if (resource == null) {
            return chain.filter(exchange);
        }

        Entry entry = null;
        try {
            entry = SphU.entry(resource);
            Entry finalEntry = entry;
            return chain.filter(exchange)
                    .doFinally(signal -> finalEntry.close());
        } catch (BlockException ex) {
            return writeError(exchange);
        } catch (RuntimeException ex) {
            if (entry != null) {
                entry.close();
            }
            throw ex;
        }
    }

    @Override
    public int getOrder() {
        return -110;
    }

    private String resourceName(ServerWebExchange exchange) {
        String path = exchange.getRequest().getPath().pathWithinApplication().value();
        HttpMethod method = exchange.getRequest().getMethod();
        if (HttpMethod.GET.equals(method) && ITEM_LIST_RESOURCE.equals(path)) {
            return ITEM_LIST_RESOURCE;
        }
        if (HttpMethod.POST.equals(method) && APPLICATION_CREATE_RESOURCE.equals(path)) {
            return APPLICATION_CREATE_RESOURCE;
        }
        return null;
    }

    private Mono<Void> writeError(ServerWebExchange exchange) {
        exchange.getResponse().setStatusCode(HttpStatus.TOO_MANY_REQUESTS);
        exchange.getResponse().getHeaders().setContentType(MediaType.APPLICATION_JSON);
        byte[] body = toJson(ApiResponse.error(ErrorCode.REQUEST_TOO_FREQUENT))
                .getBytes(StandardCharsets.UTF_8);
        DataBuffer buffer = exchange.getResponse().bufferFactory().wrap(body);
        return exchange.getResponse().writeWith(Mono.just(buffer));
    }

    private String toJson(ApiResponse<?> response) {
        try {
            return objectMapper.writeValueAsString(response);
        } catch (JsonProcessingException ex) {
            return "{\"code\":" + ErrorCode.REQUEST_TOO_FREQUENT.code()
                    + ",\"message\":\"" + ErrorCode.REQUEST_TOO_FREQUENT.message()
                    + "\",\"data\":null}";
        }
    }
}
