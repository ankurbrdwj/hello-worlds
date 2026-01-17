package com.ankur.tdd.spring.controller;

import com.ankur.tdd.spring.dto.ProductRequest;
import com.ankur.tdd.spring.dto.ProductResponse;
import com.ankur.tdd.spring.service.ProductService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.net.URI;

@RestController
@RequestMapping("/product")
@RequiredArgsConstructor

public class ProductController {

    @Autowired
    private final ProductService productService;

    @GetMapping("/{id}")
    public ResponseEntity<ProductResponse> getProduct(@PathVariable Long id) {
        ProductResponse response = productService.findProduct(id);
        return ResponseEntity.ok()
                .eTag("\"" + response.getVersion() + "\"")  // ETag with quotes
                .location(URI.create("/product/" + id))      // Location header
                .body(response);
    }

    @PostMapping()
    ProductResponse createProduct(@RequestBody ProductRequest productRequest){
       return productService.createProduct(productRequest);
    }

    @DeleteMapping("/{id}")
    ProductResponse deleteProduct(@PathVariable Long id){
        productService.deleteProduct(id);
        return null;
    }
}
