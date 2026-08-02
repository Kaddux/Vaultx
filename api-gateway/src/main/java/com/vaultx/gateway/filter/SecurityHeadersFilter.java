package com.vaultx.gateway.filter;

import org.springframework.cloud.gateway.filter.GatewayFilterChain;
import org.springframework.cloud.gateway.filter.GlobalFilter;
import org.springframework.core.Ordered;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

@Component
public class SecurityHeadersFilter implements GlobalFilter, Ordered {

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, GatewayFilterChain chain) {
        return chain.filter(exchange).doFinally(signalType -> {
            ServerHttpResponse response = exchange.getResponse();
            response.getHeaders().computeIfAbsent("X-Content-Type-Options",
                    h -> java.util.List.of("nosniff"));
            response.getHeaders().computeIfAbsent("X-Frame-Options",
                    h -> java.util.List.of("DENY"));
            response.getHeaders().computeIfAbsent("X-XSS-Protection",
                    h -> java.util.List.of("1; mode=block"));
            response.getHeaders().computeIfAbsent("Referrer-Policy",
                    h -> java.util.List.of("strict-origin-when-cross-origin"));
        });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE + 10;
    }
}
