package com.ecommerce.inventory.config;

import com.ecommerce.inventory.model.Inventory;
import com.ecommerce.inventory.repository.InventoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.time.Instant;
import java.util.List;

@Configuration
public class InventoryDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(InventoryDataInitializer.class);

    @Bean
    public CommandLineRunner initInventoryDatabase(InventoryRepository inventoryRepository) {
        return args -> {
            if (inventoryRepository.count() == 0) {
                log.info("Inventory table is empty. Seeding initial stock levels in PostgreSQL...");
                Instant now = Instant.now();

                List<Inventory> initialInventories = List.of(
                        Inventory.builder().productId("prod-101").availableQuantity(14).reservedQuantity(0).updatedAt(now).build(),
                        Inventory.builder().productId("prod-102").availableQuantity(8).reservedQuantity(0).updatedAt(now).build(),
                        Inventory.builder().productId("prod-103").availableQuantity(25).reservedQuantity(0).updatedAt(now).build(),
                        Inventory.builder().productId("prod-104").availableQuantity(30).reservedQuantity(0).updatedAt(now).build(),
                        Inventory.builder().productId("prod-105").availableQuantity(12).reservedQuantity(0).updatedAt(now).build(),
                        Inventory.builder().productId("prod-106").availableQuantity(18).reservedQuantity(0).updatedAt(now).build()
                );

                inventoryRepository.saveAll(initialInventories);
                log.info("Successfully seeded {} inventory records.", initialInventories.size());
            } else {
                log.info("Inventory table already contains {} records. Skipping seed.", inventoryRepository.count());
            }
        };
    }
}
