package com.ankur.tdd.spring.dto;

import lombok.Data;

import java.time.Instant;
@Data
public class ProductResponse {

    private final long id;
    private final String productName;
    private final Integer quantity;
    private final Instant createdAt;
    private final Instant modifiedAt;
    private final Integer version;

}
