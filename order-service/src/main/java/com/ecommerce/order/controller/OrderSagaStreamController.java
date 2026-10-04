package com.ecommerce.order.controller;

import com.ecommerce.order.service.OrderSagaEventBroadcaster;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/orders")
@Tag(name = "Saga Live Observability & SSE", description = "Real-time Server-Sent Events stream and recent Kafka event stream for live frontend Saga visualization")
public class OrderSagaStreamController {

    private final OrderSagaEventBroadcaster broadcaster;

    public OrderSagaStreamController(OrderSagaEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    @Operation(summary = "Subscribe to live Saga SSE event stream", description = "Maintains persistent HTTP Server-Sent Events connection streaming Kafka Saga state transitions in real time to the browser UI.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "SSE stream established")
    })
    public SseEmitter streamSagaEvents() {
        return broadcaster.createEmitter();
    }

    @GetMapping("/kafka-events")
    @Operation(summary = "Get recent Kafka events history", description = "Returns recent Kafka event payloads observed during Saga execution for debugging and UI monitoring.")
    @ApiResponses({
            @ApiResponse(responseCode = "200", description = "Recent event list retrieved")
    })
    public ResponseEntity<List<Map<String, Object>>> getRecentKafkaEvents() {
        return ResponseEntity.ok(broadcaster.getRecentKafkaEvents());
    }
}
