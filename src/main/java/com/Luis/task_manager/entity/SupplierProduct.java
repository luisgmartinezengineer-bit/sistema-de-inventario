package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_products",
       uniqueConstraints = @UniqueConstraint(columnNames = {"supplier_id","product_id"}))
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierProduct {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "product_id")
    private Product product;

    private BigDecimal currentPrice;
    private BigDecimal minHistoricalPrice;
    private BigDecimal maxHistoricalPrice;

    @Builder.Default
    private Integer minOrderQuantity = 1;

    private String supplierProductCode;   // código que usa el proveedor

    @Builder.Default
    private boolean preferred = false;

    private LocalDateTime lastQuoteDate;

    @Builder.Default
    private boolean active = true;
}
