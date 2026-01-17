package com.ankur.outbox.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CreateOrderRequest {
    private String customerName;
    private String product;
    private Integer quantity;
    private BigDecimal totalPrice;
}