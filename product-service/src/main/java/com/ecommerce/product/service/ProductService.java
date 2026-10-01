package com.ecommerce.product.service;

import com.ecommerce.product.dto.ProductRequest;
import com.ecommerce.product.dto.ProductResponse;

import java.util.List;

public interface ProductService {

    ProductResponse createProduct(ProductRequest request);

    List<ProductResponse> getAllProducts(String category, String brand);

    ProductResponse getProductById(String id);

    ProductResponse updateProduct(String id, ProductRequest request);

    void deleteProduct(String id);
}
