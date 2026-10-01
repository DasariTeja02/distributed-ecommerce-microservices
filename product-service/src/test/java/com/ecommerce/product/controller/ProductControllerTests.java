package com.ecommerce.product.controller;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;
import com.ecommerce.product.exception.ResourceNotFoundException;
import com.ecommerce.product.service.ProductService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.math.BigDecimal;
import java.time.Instant;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ProductController.class)
@AutoConfigureMockMvc(addFilters = false)
class ProductControllerTests {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private ProductService productService;

    @Test
    void shouldCreateProductAndReturn201() throws Exception {
        ProductRequest request = ProductRequest.builder()
                .name("Sony WH-1000XM5")
                .description("Noise cancelling headphones")
                .category("ELECTRONICS")
                .brand("Sony")
                .price(new BigDecimal("399.99"))
                .available(true)
                .build();

        ProductResponse response = ProductResponse.builder()
                .id("prod-202")
                .name("Sony WH-1000XM5")
                .description("Noise cancelling headphones")
                .category("ELECTRONICS")
                .brand("Sony")
                .price(new BigDecimal("399.99"))
                .available(true)
                .createdAt(Instant.now())
                .updatedAt(Instant.now())
                .build();

        when(productService.createProduct(any(ProductRequest.class))).thenReturn(response);

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").value("prod-202"))
                .andExpect(jsonPath("$.name").value("Sony WH-1000XM5"))
                .andExpect(jsonPath("$.price").value(399.99));
    }

    @Test
    void shouldReturn400WhenProductRequestInvalid() throws Exception {
        ProductRequest invalidRequest = ProductRequest.builder()
                .name("") // Blank name
                .description("")
                .price(new BigDecimal("-10.00")) // Invalid negative price
                .build();

        mockMvc.perform(post("/api/products")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status").value(400))
                .andExpect(jsonPath("$.validationErrors").exists());
    }

    @Test
    void shouldGetProductById() throws Exception {
        ProductResponse response = ProductResponse.builder()
                .id("prod-202")
                .name("Sony WH-1000XM5")
                .price(new BigDecimal("399.99"))
                .build();

        when(productService.getProductById("prod-202")).thenReturn(response);

        mockMvc.perform(get("/api/products/prod-202"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.id").value("prod-202"))
                .andExpect(jsonPath("$.name").value("Sony WH-1000XM5"));
    }

    @Test
    void shouldReturn404WhenProductNotFound() throws Exception {
        when(productService.getProductById("not-found"))
                .thenThrow(new ResourceNotFoundException("Product not found with id: not-found"));

        mockMvc.perform(get("/api/products/not-found"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404))
                .andExpect(jsonPath("$.message").value("Product not found with id: not-found"));
    }

    @Test
    void shouldDeleteProductAndReturn204() throws Exception {
        doNothing().when(productService).deleteProduct("prod-202");

        mockMvc.perform(delete("/api/products/prod-202"))
                .andExpect(status().isNoContent());
    }
}
