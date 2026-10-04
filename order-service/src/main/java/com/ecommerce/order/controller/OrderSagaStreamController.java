package com.ecommerce.order.controller;

import com.ecommerce.order.service.OrderSagaEventBroadcaster;
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
public class OrderSagaStreamController {

    private final OrderSagaEventBroadcaster broadcaster;

    public OrderSagaStreamController(OrderSagaEventBroadcaster broadcaster) {
        this.broadcaster = broadcaster;
    }

    @GetMapping(value = "/events/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter streamSagaEvents() {
        return broadcaster.createEmitter();
    }

    @GetMapping("/kafka-events")
    public ResponseEntity<List<Map<String, Object>>> getRecentKafkaEvents() {
        return ResponseEntity.ok(broadcaster.getRecentKafkaEvents());
    }
}
