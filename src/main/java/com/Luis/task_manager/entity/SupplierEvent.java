package com.Luis.task_manager.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;

@Entity
@Table(name = "supplier_events")
@Data @NoArgsConstructor @AllArgsConstructor @Builder
public class SupplierEvent {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // null = evento global (aplica a todos)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_product_id")
    private SupplierProduct supplierProduct;

    @Column(nullable = false)
    private String type; // TEMPORADA_ALTA, ESCASEZ, PARO, FERIA, COSECHA, OTRO

    @Column(nullable = false)
    private String description;

    @Column(nullable = false)
    private LocalDate startDate;

    private LocalDate endDate;

    private String expectedImpact; // SUBE_PRECIO, BAJA_PRECIO, AUMENTA_DEMANDA

    @Builder.Default
    private String intensity = "MEDIA"; // BAJA, MEDIA, ALTA
}
