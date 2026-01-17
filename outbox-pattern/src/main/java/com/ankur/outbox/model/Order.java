package com.ankur.outbox.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.relational.core.mapping.Table;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Table("orders")
public class Order {

    @Id
    private Long id;

    private String customerName;
    private String product;
    private Integer quantity;
    private BigDecimal totalPrice;
    private String status;
    private LocalDateTime createdAt;

    public Order(String customerName, String product, Integer quantity, BigDecimal totalPrice) {
        this.customerName = customerName;
        this.product = product;
        this.quantity = quantity;
        this.totalPrice = totalPrice;
        this.status = "CREATED";
        this.createdAt = LocalDateTime.now();
    }
}