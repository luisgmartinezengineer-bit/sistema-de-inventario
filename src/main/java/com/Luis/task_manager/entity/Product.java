package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "products")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Product {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @NotBlank
    @Column(nullable = false)
    private String name;

    private String description;

    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "category_id")
    private Category category;

    @NotNull
    @DecimalMin("0.0")
    @Column(nullable = false, precision = 12, scale = 2)
    private BigDecimal price;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer stock;

    @NotNull
    @Min(0)
    @Column(nullable = false)
    private Integer minStock;

    private String unit;

    @Column(unique = true)
    private String barcode;

    // IVA: 0, 5 o 19 (porcentaje)
    @Builder.Default
    @Column(precision = 5, scale = 2)
    private BigDecimal taxRate = new BigDecimal("19");

    private LocalDate expirationDate;

    private String lotNumber;

    @Builder.Default
    private boolean active = true;
}
