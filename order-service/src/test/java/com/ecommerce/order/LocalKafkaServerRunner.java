package com.ecommerce.order;

import org.springframework.kafka.test.EmbeddedKafkaZKBroker;

public class LocalKafkaServerRunner {
    public static void main(String[] args) throws Exception {
        System.out.println("Starting Embedded Kafka ZK Broker on port 9092...");
        EmbeddedKafkaZKBroker broker = new EmbeddedKafkaZKBroker(1, true, 1,
                "order-events", "inventory-events", "payment-events", "notification-events");
        broker.zkPort(2181);
        broker.kafkaPorts(9092);
        broker.afterPropertiesSet();
        System.out.println("=================================================");
        System.out.println(">>> EMBEDDED_KAFKA_READY_ON_PORT_9092 <<<");
        System.out.println("Brokers: " + broker.getBrokersAsString());
        System.out.println("=================================================");
        Thread.currentThread().join();
    }
}
