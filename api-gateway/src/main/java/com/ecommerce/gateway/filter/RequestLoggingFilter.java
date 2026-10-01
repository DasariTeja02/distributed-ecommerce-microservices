package com.ecommerce.gateway.filter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.core.Ordered;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ServerWebExchange;
import org.springframework.web.server.WebFilter;
import org.springframework.web.server.WebFilterChain;
import reactor.core.publisher.Mono;

import java.util.UUID;

@Component
public class RequestLoggingFilter implements WebFilter, Ordered {

    private static final Logger log = LoggerFactory.getLogger(RequestLoggingFilter.class);
    public static final String CORRELATION_ID_HEADER = "X-Correlation-Id";

    @Override
    public Mono<Void> filter(ServerWebExchange exchange, WebFilterChain chain) {
        long startTime = System.currentTimeMillis();
        String correlationId = exchange.getRequest().getHeaders().getFirst(CORRELATION_ID_HEADER);
        if (correlationId == null || correlationId.isBlank()) {
            correlationId = UUID.randomUUID().toString();
        }

        final String finalCorrelationId = correlationId;
        exchange.getResponse().getHeaders().set(CORRELATION_ID_HEADER, finalCorrelationId);

        ServerWebExchange mutatedExchange = exchange.mutate()
                .request(r -> r.header(CORRELATION_ID_HEADER, finalCorrelationId))
                .build();

        String method = mutatedExchange.getRequest().getMethod().name();
        String path = mutatedExchange.getRequest().getURI().getRawPath();
        String remoteAddress = mutatedExchange.getRequest().getRemoteAddress() != null
                ? mutatedExchange.getRequest().getRemoteAddress().getAddress().getHostAddress()
                : "UNKNOWN";

        log.info("[Gateway Inbound] CorrelationId: {} | Method: {} | Path: {} | ClientIP: {}",
                finalCorrelationId, method, path, remoteAddress);

        return chain.filter(mutatedExchange)
                .doFinally(signalType -> {
                    long duration = System.currentTimeMillis() - startTime;
                    int statusCode = mutatedExchange.getResponse().getStatusCode() != null
                            ? mutatedExchange.getResponse().getStatusCode().value()
                            : 500;

                    log.info("[Gateway Outbound] CorrelationId: {} | Status: {} | Duration: {}ms",
                            finalCorrelationId, statusCode, duration);
                });
    }

    @Override
    public int getOrder() {
        return Ordered.HIGHEST_PRECEDENCE;
    }
}
