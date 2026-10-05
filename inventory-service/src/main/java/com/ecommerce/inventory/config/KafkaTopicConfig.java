package com.ecommerce.inventory.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
@ConditionalOnProperty(name = "spring.kafka.admin.auto-create", havingValue = "true", matchIfMissing = false)
public class KafkaTopicConfig {

    public static final String INVENTORY_EVENTS_TOPIC = "inventory-events";
    public static final String INVENTORY_EVENTS_DLT = "inventory-events.DLT";

    @Bean
    public NewTopic inventoryEventsTopic() {
        return TopicBuilder.name(INVENTORY_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic inventoryEventsDlt() {
        return TopicBuilder.name(INVENTORY_EVENTS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
