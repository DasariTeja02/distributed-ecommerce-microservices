package com.ecommerce.gateway.exception;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.web.reactive.error.ErrorWebExceptionHandler;
import org.springframework.core.annotation.Order;
import org.springframework.core.io.buffer.DataBuffer;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.server.ResponseStatusException;
import org.springframework.web.server.ServerWebExchange;
import reactor.core.publisher.Mono;

import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;

@Component
@Order(-2)
public class GlobalErrorWebExceptionHandler implements ErrorWebExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalErrorWebExceptionHandler.class);
    private final ObjectMapper objectMapper = new ObjectMapper();

    @Override
    public Mono<Void> handle(ServerWebExchange exchange, Throwable ex) {
        ServerHttpResponse response = exchange.getResponse();

        if (response.isCommitted()) {
            return Mono.error(ex);
        }

        response.getHeaders().setContentType(MediaType.APPLICATION_JSON);

        HttpStatus status = HttpStatus.INTERNAL_SERVER_ERROR;
        String message = ex.getMessage();

        if (ex instanceof ResponseStatusException rse) {
            status = HttpStatus.valueOf(rse.getStatusCode().value());
            message = rse.getReason() != null ? rse.getReason() : rse.getMessage();
        }

        response.setStatusCode(status);

        Map<String, Object> errorAttributes = new LinkedHashMap<>();
        errorAttributes.put("timestamp", Instant.now().toString());
        errorAttributes.put("path", exchange.getRequest().getPath().value());
        errorAttributes.put("status", status.value());
        errorAttributes.put("error", status.getReasonPhrase());
        errorAttributes.put("message", message);

        String correlationId = exchange.getRequest().getHeaders().getFirst("X-Correlation-Id");
        if (correlationId != null) {
            errorAttributes.put("correlationId", correlationId);
        }

        log.error("[Gateway Error] Path: {} | Status: {} | Message: {}",
                exchange.getRequest().getPath().value(), status, message, ex);

        byte[] bytes;
        try {
            bytes = objectMapper.writeValueAsBytes(errorAttributes);
        } catch (JsonProcessingException e) {
            bytes = ("{\"status\":500,\"error\":\"Internal Server Error\",\"message\":\"" + message + "\"}").getBytes(StandardCharsets.UTF_8);
        }

        DataBufferFactory bufferFactory = response.bufferFactory();
        DataBuffer buffer = bufferFactory.wrap(bytes);

        return response.writeWith(Mono.just(buffer));
    }
}
