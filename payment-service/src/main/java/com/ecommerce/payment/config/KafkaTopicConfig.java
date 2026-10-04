package com.ecommerce.payment.config;

import org.apache.kafka.clients.admin.NewTopic;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.kafka.config.TopicBuilder;

import org.springframework.context.annotation.Profile;

@Configuration
@Profile("!test")
public class KafkaTopicConfig {

    public static final String PAYMENT_EVENTS_TOPIC = "payment-events";
    public static final String PAYMENT_EVENTS_DLT = "payment-events.DLT";

    @Bean
    public NewTopic paymentEventsTopic() {
        return TopicBuilder.name(PAYMENT_EVENTS_TOPIC)
                .partitions(3)
                .replicas(1)
                .build();
    }

    @Bean
    public NewTopic paymentEventsDlt() {
        return TopicBuilder.name(PAYMENT_EVENTS_DLT)
                .partitions(1)
                .replicas(1)
                .build();
    }
}
