package com.ecommerce.order.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;

@Component
public class OrderSagaEventBroadcaster {

    private static final Logger log = LoggerFactory.getLogger(OrderSagaEventBroadcaster.class);
    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();
    private final List<Map<String, Object>> recentKafkaEvents = new CopyOnWriteArrayList<>();
    private static final int MAX_HISTORY = 50;

    public SseEmitter createEmitter() {
        SseEmitter emitter = new SseEmitter(600_000L); // 10 minutes timeout
        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        try {
            // Send initial ping
            emitter.send(SseEmitter.event().name("INIT").data("Connected to Saga Live Stream"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcast(String eventName, Object data) {
        log.info("[Saga Stream Broadcast] Event: {}", eventName);

        // Record for Kafka event history
        if (data instanceof Map) {
            @SuppressWarnings("unchecked")
            Map<String, Object> map = (Map<String, Object>) data;
            recordEvent(map);
        }

        List<SseEmitter> deadEmitters = new ArrayList<>();
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name(eventName).data(data));
            } catch (Exception e) {
                deadEmitters.add(emitter);
            }
        }
        emitters.removeAll(deadEmitters);
    }

    public void recordEvent(Map<String, Object> eventRecord) {
        recentKafkaEvents.add(0, eventRecord);
        while (recentKafkaEvents.size() > MAX_HISTORY) {
            recentKafkaEvents.remove(recentKafkaEvents.size() - 1);
        }
    }

    public List<Map<String, Object>> getRecentKafkaEvents() {
        return Collections.unmodifiableList(recentKafkaEvents);
    }
}
