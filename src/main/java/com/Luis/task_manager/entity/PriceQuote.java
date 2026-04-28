package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "price_quotes")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class PriceQuote {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "supplier_product_id")
    private SupplierProduct supplierProduct;

    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @Column(nullable = false)
    private LocalDateTime date;

    @Builder.Default
    private String origin = "MANUAL"; // MANUAL, EMAIL, IMPORTADO

    private String note;

    private BigDecimal variationPercent; // calculado al guardar vs. cotización anterior

    @Builder.Default
    private boolean valid = true; // permite marcar outliers sin borrar
}
