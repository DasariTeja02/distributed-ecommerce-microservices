package com.ecommerce.product;

import com.ecommerce.product.repository.ProductRepository;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;

@SpringBootTest(properties = {
        "eureka.client.enabled=false",
        "spring.data.mongodb.uri=mongodb://localhost:27017/test"
})
class ProductServiceApplicationTests {

    @MockBean
    private ProductRepository productRepository;

    @Test
    void contextLoads() {
    }
}
