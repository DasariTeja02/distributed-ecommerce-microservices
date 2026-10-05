package com.ecommerce.product.service;

import com.ecommerce.product.config.ProductDataInitializer;
import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;

import java.time.Instant;
import java.util.List;
import java.util.stream.Collectors;

@Service
public class ProductServiceImpl implements ProductService {

    private static final Logger log = LoggerFactory.getLogger(ProductServiceImpl.class);
    private final ProductRepository productRepository;

    public ProductServiceImpl(ProductRepository productRepository) {
        this.productRepository = productRepository;
    }

    @Override
    public ProductResponse createProduct(ProductRequest request) {
        log.info("Creating new product with name: {}", request.getName());

        Instant now = Instant.now();
        Product product = Product.builder()
                .name(request.getName().trim())
                .description(request.getDescription().trim())
                .category(request.getCategory().trim().toUpperCase())
                .brand(request.getBrand().trim())
                .price(request.getPrice())
                .available(request.isAvailable())
                .imageUrl(request.getImageUrl())
                .createdAt(now)
                .updatedAt(now)
                .build();

        Product saved = productRepository.save(product);
        log.info("Product created successfully with id: {}", saved.getId());
        return mapToResponse(saved);
    }

    @Override
    public List<ProductResponse> getAllProducts(String category, String brand) {
        log.info("Fetching products with filters - category: {}, brand: {}", category, brand);
        List<Product> products;

        try {
            if (category != null && !category.isBlank()) {
                products = productRepository.findByCategory(category.trim().toUpperCase());
            } else if (brand != null && !brand.isBlank()) {
                products = productRepository.findByBrand(brand.trim());
            } else {
                products = productRepository.findAll();
            }
        } catch (Exception e) {
            log.warn("MongoDB unavailable ({}), falling back to in-memory product catalog.", e.getMessage());
            products = ProductDataInitializer.CATALOG;
            if (category != null && !category.isBlank()) {
                String catFilter = category.trim().toUpperCase();
                products = products.stream().filter(p -> catFilter.equalsIgnoreCase(p.getCategory())).collect(Collectors.toList());
            } else if (brand != null && !brand.isBlank()) {
                String brandFilter = brand.trim().toUpperCase();
                products = products.stream().filter(p -> brandFilter.equalsIgnoreCase(p.getBrand())).collect(Collectors.toList());
            }
        }

        return products.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Override
    public ProductResponse getProductById(String id) {
        log.info("Fetching product with id: {}", id);
        Product product;
        try {
            product = productRepository.findById(id)
                    .orElseGet(() -> ProductDataInitializer.CATALOG.stream()
                            .filter(p -> p.getId().equalsIgnoreCase(id))
                            .findFirst()
                            .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id)));
        } catch (ResourceNotFoundException rnfe) {
            throw rnfe;
        } catch (Exception e) {
            log.warn("MongoDB unavailable ({}), finding product in fallback catalog: {}", e.getMessage(), id);
            product = ProductDataInitializer.CATALOG.stream()
                    .filter(p -> p.getId().equalsIgnoreCase(id))
                    .findFirst()
                    .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));
        }
        return mapToResponse(product);
    }

    @Override
    public ProductResponse updateProduct(String id, ProductRequest request) {
        log.info("Updating product with id: {}", id);
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Product not found with id: " + id));

        product.setName(request.getName().trim());
        product.setDescription(request.getDescription().trim());
        product.setCategory(request.getCategory().trim().toUpperCase());
        product.setBrand(request.getBrand().trim());
        product.setPrice(request.getPrice());
        product.setAvailable(request.isAvailable());
        if (request.getImageUrl() != null && !request.getImageUrl().isBlank()) {
            product.setImageUrl(request.getImageUrl());
        }
        product.setUpdatedAt(Instant.now());

        Product updated = productRepository.save(product);
        log.info("Product updated successfully with id: {}", updated.getId());
        return mapToResponse(updated);
    }

    @Override
    public void deleteProduct(String id) {
        log.info("Deleting product with id: {}", id);
        if (!productRepository.existsById(id)) {
            throw new ResourceNotFoundException("Product not found with id: " + id);
        }
        productRepository.deleteById(id);
        log.info("Product deleted successfully with id: {}", id);
    }

    private ProductResponse mapToResponse(Product product) {
        return ProductResponse.builder()
                .id(product.getId())
                .name(product.getName())
                .description(product.getDescription())
                .category(product.getCategory())
                .brand(product.getBrand())
                .price(product.getPrice())
                .available(product.isAvailable())
                .imageUrl(product.getImageUrl())
                .createdAt(product.getCreatedAt())
                .updatedAt(product.getUpdatedAt())
                .build();
    }
}
