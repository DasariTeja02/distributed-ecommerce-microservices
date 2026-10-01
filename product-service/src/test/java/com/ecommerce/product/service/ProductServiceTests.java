package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.model.Product;
import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ProductServiceTests {

    @Mock
    private ProductRepository productRepository;

    @InjectMocks
    private ProductServiceImpl productService;

    private Product product;
    private ProductRequest productRequest;

    @BeforeEach
    void setUp() {
        product = Product.builder()
                .id("prod-101")
                .name("MacBook Pro 16")
                .description("Apple M3 Max Chip")
                .category("ELECTRONICS")
                .brand("Apple")
                .price(new BigDecimal("3499.00"))
                .available(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        productRequest = ProductRequest.builder()
                .name("MacBook Pro 16")
                .description("Apple M3 Max Chip")
                .category("ELECTRONICS")
                .brand("Apple")
                .price(new BigDecimal("3499.00"))
                .available(true)
                .build();
    }

    @Test
    void shouldCreateProductSuccessfully() {
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductResponse response = productService.createProduct(productRequest);

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("prod-101");
        assertThat(response.getName()).isEqualTo("MacBook Pro 16");
        assertThat(response.getPrice()).isEqualTo(new BigDecimal("3499.00"));
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void shouldGetProductByIdSuccessfully() {
        when(productRepository.findById("prod-101")).thenReturn(Optional.of(product));

        ProductResponse response = productService.getProductById("prod-101");

        assertThat(response).isNotNull();
        assertThat(response.getId()).isEqualTo("prod-101");
        assertThat(response.getName()).isEqualTo("MacBook Pro 16");
    }

    @Test
    void shouldThrowExceptionWhenProductNotFound() {
        when(productRepository.findById("invalid-id")).thenReturn(Optional.empty());

        assertThatThrownBy(() -> productService.getProductById("invalid-id"))
                .isInstanceOf(ResourceNotFoundException.class)
                .hasMessageContaining("Product not found with id: invalid-id");
    }

    @Test
    void shouldGetAllProducts() {
        when(productRepository.findAll()).thenReturn(List.of(product));

        List<ProductResponse> responses = productService.getAllProducts(null, null);

        assertThat(responses).hasSize(1);
        assertThat(responses.get(0).getName()).isEqualTo("MacBook Pro 16");
    }

    @Test
    void shouldUpdateProductSuccessfully() {
        when(productRepository.findById("prod-101")).thenReturn(Optional.of(product));
        when(productRepository.save(any(Product.class))).thenReturn(product);

        ProductRequest updateRequest = ProductRequest.builder()
                .name("MacBook Pro 16 M3 Max")
                .description("Updated specs")
                .category("ELECTRONICS")
                .brand("Apple")
                .price(new BigDecimal("3699.00"))
                .available(true)
                .build();

        ProductResponse response = productService.updateProduct("prod-101", updateRequest);

        assertThat(response).isNotNull();
        verify(productRepository, times(1)).save(any(Product.class));
    }

    @Test
    void shouldDeleteProductSuccessfully() {
        when(productRepository.existsById("prod-101")).thenReturn(true);
        doNothing().when(productRepository).deleteById("prod-101");

        productService.deleteProduct("prod-101");

        verify(productRepository, times(1)).deleteById("prod-101");
    }

    @Test
    void shouldThrowWhenDeletingNonExistingProduct() {
        when(productRepository.existsById("prod-999")).thenReturn(false);

        assertThatThrownBy(() -> productService.deleteProduct("prod-999"))
                .isInstanceOf(ResourceNotFoundException.class);
    }
}
