package com.ankur.tdd.spring.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Table;
import jakarta.persistence.criteria.CriteriaBuilder;
import lombok.*;
import lombok.experimental.SuperBuilder;

@SuperBuilder
@Entity
@Getter
@Setter
@Table(name = "product")
@EqualsAndHashCode(callSuper = true)
public class Product extends AbstractEntity{

    @Column(nullable = false)
    private String productName;
    @Column(nullable = false)
    private Integer quantity;

}
