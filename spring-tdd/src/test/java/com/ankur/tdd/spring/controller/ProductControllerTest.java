package com.ankur.tdd.spring.controller;

import com.ankur.tdd.spring.dto.ProductRequest;
import com.ankur.tdd.spring.dto.ProductResponse;
import com.ankur.tdd.spring.entity.Product;
import com.ankur.tdd.spring.service.ProductService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.context.junit.jupiter.SpringExtension;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.hamcrest.core.Is.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@ExtendWith(SpringExtension.class)
@SpringBootTest
@AutoConfigureMockMvc
public class ProductControllerTest {

    @MockitoBean
    private ProductService productService;
    @Autowired
    private MockMvc mockMvc;

    Product saved;
    ProductResponse productResponse;

    @BeforeEach
    void setup() {
        saved = Product.builder()
                .id(1L)
                .productName("Product Name")
                .quantity(10)
                .version(1)
                .build();
        productResponse = new ProductResponse(
                saved.getId(),
                saved.getProductName(),
                saved.getQuantity(),
                saved.getCreatedAt(),
                saved.getModifiedAt(),
                saved.getVersion());
    }

    @Test
    @DisplayName("GET /product/1 - Found")
    void testGetProductByIdFound() throws Exception {

        when(productService.findProduct(1L)).thenReturn(productResponse);
        mockMvc.perform(get("/product/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/product/1"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Product Name")))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.version", is(1)));
    }

    @Test
    @DisplayName("POST /product/ - Found")
    void testCreateProduct() throws Exception {
        when(productService.createProduct(any(ProductRequest.class))).thenReturn(productResponse);
        mockMvc.perform(post("/product")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("""
                                {
                                  "productName": "Product Name",
                                  "quantity": 10
                                }
                                """))
                .andExpect(status().isCreated())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.LOCATION, "/product/1"))
                .andExpect(jsonPath("$.id").value(1))
                .andExpect(jsonPath("$.productName").value("Product Name"))
                .andExpect(jsonPath("$.quantity").value(10))
                .andExpect(jsonPath("$.version").value(1));
    }

    @Test
    @DisplayName("GET /product/1 - Found")
    void testDeleteProductByIdFound() throws Exception {

        when(productService.findProduct(1L)).thenReturn(productResponse);
        mockMvc.perform(get("/product/{id}", 1L))
                .andExpect(status().isOk())
                .andExpect(content().contentType(MediaType.APPLICATION_JSON))
                .andExpect(header().string(HttpHeaders.ETAG, "\"1\""))
                .andExpect(header().string(HttpHeaders.LOCATION, "/product/1"))
                .andExpect(jsonPath("$.id", is(1)))
                .andExpect(jsonPath("$.name", is("Product Name")))
                .andExpect(jsonPath("$.quantity", is(10)))
                .andExpect(jsonPath("$.version", is(1)));
    }
}
