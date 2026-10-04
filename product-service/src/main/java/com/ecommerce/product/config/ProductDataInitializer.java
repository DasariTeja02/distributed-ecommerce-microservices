package com.ecommerce.product.config;

import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

@Configuration
public class ProductDataInitializer {

    private static final Logger log = LoggerFactory.getLogger(ProductDataInitializer.class);

    @Bean
    public CommandLineRunner initProductDatabase(ProductRepository productRepository) {
        return args -> {
            if (productRepository.count() == 0) {
                log.info("MongoDB product collection is empty. Seeding initial e-commerce catalog...");
                Instant now = Instant.now();

                List<Product> initialProducts = List.of(
                        Product.builder()
                                .id("prod-101")
                                .name("Apple MacBook Pro 16\" M3 Max")
                                .description("16-core CPU, 40-core GPU, 48GB Unified Memory, 1TB SSD Storage.")
                                .category("LAPTOPS")
                                .brand("Apple")
                                .price(new BigDecimal("3499.00"))
                                .available(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        Product.builder()
                                .id("prod-102")
                                .name("Dell XPS 15 OLED Touch")
                                .description("Intel Core i9-13900H, 32GB RAM, 1TB SSD, 3.5K OLED InfinityEdge.")
                                .category("LAPTOPS")
                                .brand("Dell")
                                .price(new BigDecimal("2199.00"))
                                .available(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        Product.builder()
                                .id("prod-103")
                                .name("Samsung Galaxy S24 Ultra")
                                .description("Snapdragon 8 Gen 3, 200MP Camera, Galaxy AI, Titanium Gray, 512GB.")
                                .category("SMARTPHONES")
                                .brand("Samsung")
                                .price(new BigDecimal("1299.99"))
                                .available(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        Product.builder()
                                .id("prod-104")
                                .name("Sony WH-1000XM5 Headphones")
                                .description("Industry-leading noise canceling with Auto NC Optimizer, 30h battery.")
                                .category("AUDIO")
                                .brand("Sony")
                                .price(new BigDecimal("398.00"))
                                .available(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        Product.builder()
                                .id("prod-105")
                                .name("Apple Watch Ultra 2 Titanium")
                                .description("Rugged 49mm titanium case, precision dual-frequency GPS, 100m water resistant.")
                                .category("WEARABLES")
                                .brand("Apple")
                                .price(new BigDecimal("799.00"))
                                .available(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build(),
                        Product.builder()
                                .id("prod-106")
                                .name("Bose QuietComfort Ultra")
                                .description("Spatial audio with CustomTune sound calibration and active noise cancellation.")
                                .category("AUDIO")
                                .brand("Bose")
                                .price(new BigDecimal("429.00"))
                                .available(true)
                                .createdAt(now)
                                .updatedAt(now)
                                .build()
                );

                productRepository.saveAll(initialProducts);
                log.info("Successfully seeded {} products into MongoDB catalog.", initialProducts.size());
            } else {
                log.info("MongoDB product collection already contains data ({} items). Skipping seed.", productRepository.count());
            }
        };
    }
}
