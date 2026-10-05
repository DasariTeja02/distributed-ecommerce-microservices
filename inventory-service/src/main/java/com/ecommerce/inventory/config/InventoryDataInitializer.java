package com.ecommerce.inventory.config;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@Configuration
public class InventoryDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(InventoryDataInitializer.class);

    @Bean
    public CommandLineRunner initInventoryDatabase(InventoryRepository inventoryRepository) {
        return args -> {
            try {
                Instant now = Instant.now();
                int inserted = 0;
                for (int i = 101; i <= 170; i++) {
                    String prodId = "prod-" + i;
                    if (!inventoryRepository.existsByProductId(prodId)) {
                        // Deterministic realistic stock quantity (between 12 and 60)
                        int stock = 15 + ((i * 7) % 45);
                        inventoryRepository.save(Inventory.builder()
                                .productId(prodId)
                                .availableQuantity(stock)
                                .reservedQuantity(0)
                                .updatedAt(now)
                                .build());
                        inserted++;
                    }
                }
                log.info("Inventory stock synchronization completed: {} new items seeded. Total inventory records: {}", 
                    inserted, inventoryRepository.count());
            } catch (Exception e) {
                log.warn("PostgreSQL inventory table unreachable ({}), inventory service running with fallback.", e.getMessage());
            }
        };
    }
}
