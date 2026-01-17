package com.ankur.tdd.spring.dto;

import lombok.Data;

@Data
public class ProductRequest {
    private final String productName;
    private final Integer quantity;
}
