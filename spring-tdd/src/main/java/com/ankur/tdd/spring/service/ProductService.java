package com.ankur.tdd.spring.service;

import com.ankur.tdd.spring.dto.ProductRequest;
import com.ankur.tdd.spring.dto.ProductResponse;
import com.ankur.tdd.spring.entity.Product;
import com.ankur.tdd.spring.exception.ProductNotFoundException;
import com.ankur.tdd.spring.repository.ProductRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
@RequiredArgsConstructor
public class ProductService {
    @Autowired
    private final ProductRepository productRepository;

    public ProductResponse findProduct(Long id) {
        Product product = productRepository.findById(id)
                .orElseThrow(() -> new ProductNotFoundException(id));

        return mapToProductResponse(product);
    }

    private ProductResponse mapToProductResponse(Product product) {
        return new ProductResponse(
                product.getId(),
                product.getProductName(),
                product.getQuantity(),
                product.getCreatedAt(),
                product.getModifiedAt(),
                product.getVersion()
        );
    }

        public ProductResponse createProduct (ProductRequest productRequest){
            Product product = Product.builder()
                    .productName(productRequest.getProductName())
                    .quantity(productRequest.getQuantity())
                    .build();

            Product saved = productRepository.save(product);

            return new ProductResponse(
                    saved.getId(),
                    saved.getProductName(),
                    saved.getQuantity(),
                    saved.getCreatedAt(),
                    saved.getModifiedAt(),
                    saved.getVersion()
            );
        }

        public ProductResponse deleteProduct (Long id){
            return null;
        }
    }
