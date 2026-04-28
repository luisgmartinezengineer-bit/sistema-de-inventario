package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "supplier_alerts")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierAlert {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.EAGER, optional = false)
    @JoinColumn(name = "supplier_product_id")
    private SupplierProduct supplierProduct;

    // SUBIDA_PRECIO | TENDENCIA_ALCISTA | MINIMO_HISTORICO |
    // PREDICCION_SUBIDA | SIN_COTIZAR | MARGEN_EN_RIESGO
    @Column(nullable = false)
    private String type;

    @Column(nullable = false)
    private String message;

    private BigDecimal referenceValue; // el número que disparó la alerta

    @Builder.Default
    private boolean resolved = false;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    private LocalDate estimatedEventDate; // para alertas predictivas

    @Builder.Default
    private boolean emailSent = false;
}
